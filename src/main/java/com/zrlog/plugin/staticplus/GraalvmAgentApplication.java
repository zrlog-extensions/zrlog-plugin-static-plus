package com.zrlog.plugin.staticplus;

import com.zrlog.plugin.RunConstants;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.common.LoggerUtil;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.common.vo.UploadFile;
import com.zrlog.plugin.staticplus.controller.StaticPlusController;
import com.zrlog.plugin.staticplus.sync.GitFileManageImpl;
import com.zrlog.plugin.staticplus.sync.vo.CreateFileInfoVO;
import com.zrlog.plugin.staticplus.sync.vo.GitRemoteInfo;
import com.zrlog.plugin.type.RunType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GraalvmAgentApplication {

    private static final Logger LOGGER = LoggerUtil.getLogger(GraalvmAgentApplication.class);

    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
        RunConstants.runType = RunType.AGENT;
        String basePath = System.getProperty("user.dir").replace("\\target", "").replace("/target", "");
        File file = new File(basePath + "/src/main/resources");
        PluginNativeImageUtils.doLoopResourceLoad(file.listFiles(), file.getPath() + "/", "/");
        PluginNativeImageUtils.exposeController(Collections.singletonList(StaticPlusController.class));
        try {
            File testFile = File.createTempFile("test", "");
            IOUtil.writeBytesToFile(("Test content " + testFile.toURI()).getBytes(), testFile);
            List<UploadFile> list = new ArrayList<>();
            UploadFile uploadFile = new UploadFile();
            uploadFile.setFile(testFile);
            uploadFile.setFileKey(System.currentTimeMillis() + ".tmp");
            list.add(uploadFile);
            new GitFileManageImpl("{'url':'https://github.com/94fzb/zrlog-plugin-static-plus','branch':'main','username':'94fzb','password':'pwd'}", list, null).doSync();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Warm up git sync failed", e);
        }
        try {
            PluginNativeImageUtils.usedGsonObject();
            PluginNativeImageUtils.gsonNativeAgentByClazz(Arrays.asList(CreateFileInfoVO.class, GitRemoteInfo.class));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Warm up static plus native image metadata failed", e);
        }
        Application.main(args);

    }
}
