import {useState} from "react";
import {createRoot} from "react-dom/client";
import AppBase from "./AppBase";
import {PluginThemeProvider} from "./PluginThemeProvider";
import type {PluginAppearance} from "./PluginThemeProvider";

export interface StaticPlusConfig extends PluginAppearance {
    syncTemplate?: string;
    syncHtml?: string;
    syncAttached?: string;
    syncRemoteType?: string;
    git?: string;
    s3?: string;
    syncHistory?: string;
    adminColorPrimary?: string;
}

const loadFromDocument = (): StaticPlusConfig | null => {
    try {
        const node = document.getElementById("data");
        if (node === null || node.innerText.length === 0) {
            return null;
        }
        return JSON.parse(node.innerText) as StaticPlusConfig;
    } catch (e) {
        console.error("Failed to parse config JSON", e);
        return null;
    }
}

const Index = () => {
    const [config] = useState<StaticPlusConfig | null>(loadFromDocument);
    const safeConfig = config || {};

    return (
        <PluginThemeProvider page={{...safeConfig, dark: document.body.classList.contains("dark")}}>
            <AppBase config={safeConfig}/>
        </PluginThemeProvider>
    );
};

const container = document.getElementById("app");
const root = createRoot(container!);
root.render(<Index/>);
