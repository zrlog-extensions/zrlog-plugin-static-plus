package com.zrlog.plugin.staticplus.sync;

import com.google.gson.Gson;
import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.common.FileUtils;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.vo.UploadFile;
import com.zrlog.plugin.staticplus.sync.vo.GitRemoteInfo;
import com.zrlog.plugin.type.RunType;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class GitFileManageImpl implements FileManage {

    private static final Logger LOGGER = LoggerUtil.getLogger(GitFileManageImpl.class);

    private static final Lock lock = new ReentrantLock();
    private static final Lock createLock = new ReentrantLock();

    private final GitRemoteInfo gitRemoteInfo;
    private final List<UploadFile> syncFiles;
    private Git git;
    private final UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider;
    private final File repoDir;
    private static final ProxySelector defaultProxySelector = ProxySelector.getDefault();
    private final PersonIdent committerAuthor;

    public GitFileManageImpl(String configJsonStr, List<UploadFile> syncFiles) {
        this.gitRemoteInfo = new Gson().fromJson(configJsonStr, GitRemoteInfo.class);
        this.syncFiles = syncFiles;
        this.usernamePasswordCredentialsProvider = new UsernamePasswordCredentialsProvider(gitRemoteInfo.getUsername(), gitRemoteInfo.getPassword());
        String cloneDirectoryPath = System.getProperty("user.dir") + "/" + new File(URI.create(gitRemoteInfo.getUrl()).getPath()).getName();
        this.repoDir = new File(cloneDirectoryPath);
        this.committerAuthor = new PersonIdent(Objects.requireNonNullElse(gitRemoteInfo.getGitCommitterUsername(), "static-plus-robot"), Objects.requireNonNullElse(gitRemoteInfo.getGitCommitterEmail(), "static-plus-robot@zrlog.com"));
    }

    private void checkout(Git git, String branchName, UsernamePasswordCredentialsProvider usernamePasswordCredentialsProvider) throws IOException, GitAPIException {
        if (Objects.equals(git.getRepository().getBranch(), branchName)) {
            return;
        }
        if (git.lsRemote().setCredentialsProvider(usernamePasswordCredentialsProvider).call().isEmpty()) {
            File exampleFile = new File(git.getRepository().getDirectory().getParentFile(), "README.md");
            IOUtil.writeBytesToFile("# ZrLog static repository".getBytes(), exampleFile);
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Initial commit for checkout").call();
            git.checkout().setCreateBranch(true).setName(branchName).call();
            git.add().addFilepattern(".").call();
            git.commit().setCommitter(committerAuthor).setMessage("Initial commit").call();
            LOGGER.info("Init commit and checked out new branch: " + branchName);
            return;
        }
        try {
            // 1. 检查分支是否存在
            List<Ref> branches = git.branchList().call();
            boolean branchExists = branches.stream().anyMatch(ref -> ref.getName().equals("refs/heads/" + branchName));

            if (!branchExists) {
                // 2. 创建新分支
                git.checkout().setCreateBranch(true).setName(branchName).call();
                LOGGER.info("Created and checked out new branch: " + branchName);
            } else {
                // 3. 切换到指定分支
                git.checkout().setName(branchName).call();
                LOGGER.info("Checked out existing branch: " + branchName);
            }
        } catch (Exception e) {
            LOGGER.warning("Checked out branch: error " + e.getMessage());
        }
    }

    private void initGit() throws IOException, GitAPIException {
        if (RunConstants.runType == RunType.DEV) {
            LOGGER.info("Git work path: " + repoDir);
        }
        if (repoDir.exists() && new File(repoDir, ".git").exists()) {
            // 存在仓库目录，执行 pull 操作
            this.git = Git.open(repoDir);
        } else {
            // 不存在仓库目录，执行 clone 操作
            this.git = Git.cloneRepository().setDepth(1).setURI(gitRemoteInfo.getUrl())
                    .setDirectory(repoDir)
                    .setCredentialsProvider(usernamePasswordCredentialsProvider)
                    .call();
        }

    }

    private void setupProxy() {
        if (Objects.isNull(gitRemoteInfo.getProxyHttpHost()) || Objects.isNull(gitRemoteInfo.getProxyHttpPort())) {
            ProxySelector.setDefault(defaultProxySelector);
            return;
        }
        // 创建代理
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(gitRemoteInfo.getProxyHttpHost(), gitRemoteInfo.getProxyHttpPort()));
        // 设置自定义的 ProxySelector
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                if ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme())) {
                    // 对 Git 操作生效
                    if (Objects.equals(URI.create(gitRemoteInfo.getUrl()).getHost(), uri.getHost())) {
                        return List.of(proxy);
                    }
                    // 对访问 url 生效
                    if (Objects.nonNull(gitRemoteInfo.getAccessBaseUrl()) && Objects.equals(URI.create(gitRemoteInfo.getAccessBaseUrl()).getHost(), uri.getHost())) {
                        return List.of(proxy);
                    }
                }
                return defaultProxySelector.select(uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                LOGGER.warning("Proxy connection failed: " + ioe.getMessage());
                defaultProxySelector.connectFailed(uri, sa, ioe);
            }
        });
        LOGGER.info("Git work with proxy: " + gitRemoteInfo.getProxyHttpHost() + ":" + gitRemoteInfo.getProxyHttpPort());
    }

    @Override
    public boolean doSync() {
        return doSyncByUploadFiles(syncFiles);
    }

    private boolean doSyncByUploadFiles(List<UploadFile> files) {
        if (Objects.isNull(gitRemoteInfo.getUrl())) {
            return false;
        }
        if (Objects.isNull(files) || files.isEmpty()) {
            return false;
        }
        lock.lock();
        try {
            setupProxy();
            initGit();
            //检出分支
            checkout(git, gitRemoteInfo.getBranch(), usernamePasswordCredentialsProvider);
            try {
                git.pull().setCredentialsProvider(usernamePasswordCredentialsProvider).setRemote("origin").setRemoteBranchName(gitRemoteInfo.getBranch()).call();
            } catch (Exception e) {
                LoggerUtil.getLogger(GitFileManageImpl.class).warning("Git [sync] pull error " + e.getMessage());
            }
            git.remoteAdd().setName("origin");
            long start = System.currentTimeMillis();
            for (UploadFile e : files) {
                FileUtils.moveOrCopyFile(e.getFile().toString(), new File(repoDir + "/" + e.getFileKey()).toString(), false);
                if (e.getFileKey().startsWith("/")) {
                    git.add().addFilepattern(e.getFileKey().substring(1)).call();
                } else {
                    git.add().addFilepattern(e.getFileKey()).call();
                }
            }
            LOGGER.info("Git add used time " + (System.currentTimeMillis() - start) + "ms");

            git.commit().setCommitter(committerAuthor).setMessage("static-plus plugin auto commit").call();
            git.push()
                    .setCredentialsProvider(usernamePasswordCredentialsProvider)
                    .setRemote("origin")
                    .setRefSpecs(new RefSpec(gitRemoteInfo.getBranch() + ":" + gitRemoteInfo.getBranch()))
                    .call();
            LOGGER.info("Git push success");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LoggerUtil.getLogger(GitFileManageImpl.class).warning("Git [sync] push error " + e.getMessage());
            return false;
        } finally {
            lock.unlock();
        }
    }


    public static void main(String[] args) throws Exception {
        RunConstants.runType = RunType.DEV;
        UploadFile uploadFile = new UploadFile();
        File testFile = File.createTempFile("test", "");
        IOUtil.writeBytesToFile(("Test content " + testFile.toURI()).getBytes(), testFile);
        uploadFile.setFile(testFile);
        uploadFile.setFileKey(System.currentTimeMillis() + ".tmp");
        uploadFile.setRefresh(true);
        try (GitFileManageImpl gitFileManage = new GitFileManageImpl(IOUtil.getStringInputStream(GitFileManageImpl.class.getResourceAsStream("/test-git-remove-info.json")), new ArrayList<>())) {
            gitFileManage.create(testFile, uploadFile.getFileKey(), true, true);
        }
    }

    @Override
    public String create(File file, String key, boolean deleteRepeat, boolean supportHttps) throws Exception {
        createLock.lock();
        try {
            List<UploadFile> fileList = new ArrayList<>();
            UploadFile uploadFile = new UploadFile();
            uploadFile.setFile(file);
            uploadFile.setFileKey(key);
            uploadFile.setRefresh(false);
            fileList.add(uploadFile);
            UploadFile uploadFileJson = new UploadFile();
            File buildFile = new File(System.getProperty("java.io.tmpdir") + "/" + System.currentTimeMillis() + "/create-file-info.json");
            buildFile.getParentFile().mkdirs();
            String jsonStr = new Gson().toJson(Map.of("uploadDate", System.currentTimeMillis()));
            IOUtil.writeStrToFile(jsonStr, buildFile);
            uploadFileJson.setFile(buildFile);
            uploadFileJson.setFileKey(buildFile.getName());
            uploadFileJson.setRefresh(false);
            fileList.add(uploadFileJson);
            doSyncByUploadFiles(fileList);
            if (Objects.nonNull(gitRemoteInfo.getAccessBaseUrl()) && !gitRemoteInfo.getAccessBaseUrl().isEmpty()) {
                SyncUtils.checkFileSyncs(gitRemoteInfo.getAccessBaseUrl() + "/" + buildFile.getName(), jsonStr);
            }
            if (gitRemoteInfo.getAccessBaseUrl().endsWith("/")) {
                return gitRemoteInfo.getAccessBaseUrl() + key;
            }
            return gitRemoteInfo.getAccessBaseUrl() + "/" + key;
        } finally {
            createLock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        if (Objects.nonNull(git)) {
            git.close();
        }
    }
}
