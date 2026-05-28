import {legacyLogicalPropertiesTransformer, StyleProvider} from "@ant-design/cssinjs";
import {App, ConfigProvider, theme} from "antd";
import zhCN from "antd/es/locale/zh_CN";
import {useEffect, useState} from "react";
import {createRoot} from "react-dom/client";
import AppBase from "./AppBase";
import "./style.css";

const {darkAlgorithm, defaultAlgorithm} = theme;

export interface StaticPlusConfig {
    syncTemplate?: string;
    syncHtml?: string;
    syncAttached?: string;
    syncRemoteType?: string;
    git?: string;
    syncHistory?: string;
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
    const [isDark, setIsDark] = useState<boolean>(false);

    useEffect(() => {
        const checkDarkMode = () => {
            const hasDarkClass = document.body.classList.contains("dark");
            setIsDark(hasDarkClass);
        };
        checkDarkMode();

        const observer = new MutationObserver(checkDarkMode);
        observer.observe(document.body, { attributes: true, attributeFilter: ["class"] });
        return () => observer.disconnect();
    }, []);

    const safeConfig = config || {};

    return (
        <ConfigProvider
            locale={zhCN}
            theme={{
                algorithm: isDark ? darkAlgorithm : defaultAlgorithm,
            }}
        >
            <StyleProvider transformers={[legacyLogicalPropertiesTransformer]}>
                <App>
                    <AppBase config={safeConfig} />
                </App>
            </StyleProvider>
        </ConfigProvider>
    );
};

const container = document.getElementById("app");
const root = createRoot(container!);
root.render(<Index/>);
