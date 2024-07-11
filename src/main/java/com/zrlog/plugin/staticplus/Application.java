package com.zrlog.plugin.staticplus;


import com.zrlog.plugin.client.NioClient;
import com.zrlog.plugin.render.SimpleTemplateRender;
import com.zrlog.plugin.staticplus.controller.StaticPlusController;
import com.zrlog.plugin.staticplus.handle.ConnectHandler;
import com.zrlog.plugin.staticplus.service.UploadService;
import com.zrlog.plugin.staticplus.service.UploadToPrivateService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Application {


    /**
     * @param args
     */
    public static void main(String[] args) throws IOException {
        List<Class> classList = new ArrayList<>();
        ConnectHandler connectHandler = new ConnectHandler();
        classList.add(StaticPlusController.class);
        new NioClient(connectHandler, new SimpleTemplateRender(), new StaticPlusClientActionHandler(connectHandler)).connectServer(args, classList, StaticPlusPluginAction.class, Arrays.asList(UploadService.class, UploadToPrivateService.class));
    }
}

