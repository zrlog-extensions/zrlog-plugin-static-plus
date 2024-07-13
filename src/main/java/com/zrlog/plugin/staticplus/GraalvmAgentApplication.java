package com.zrlog.plugin.staticplus;

import com.google.gson.Gson;
import com.zrlog.plugin.common.IOUtil;
import com.zrlog.plugin.common.PluginNativeImageUtils;
import com.zrlog.plugin.common.vo.UploadFile;
import com.zrlog.plugin.data.codec.HttpRequestInfo;
import com.zrlog.plugin.message.Plugin;
import com.zrlog.plugin.staticplus.controller.StaticPlusController;
import com.zrlog.plugin.staticplus.service.UploadService;
import com.zrlog.plugin.staticplus.service.UploadToPrivateService;
import com.zrlog.plugin.staticplus.sync.GitFileManageImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GraalvmAgentApplication {


    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
        new Gson().toJson(new HttpRequestInfo());
        new Gson().toJson(new Plugin());
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
            new GitFileManageImpl("{'url':'https://github.com/94fzb/zrlog-plugin-static-plus','branch':'main','username':'94fzb','password':'pwd'}", list).doSync();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            PluginNativeImageUtils.usedGsonObject();
        } catch (Exception e) {
            e.printStackTrace();
        }
        UploadService.class.newInstance();
        UploadToPrivateService.class.newInstance();
        Application.main(args);

    }
}