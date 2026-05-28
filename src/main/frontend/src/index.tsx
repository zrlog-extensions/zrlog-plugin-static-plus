import {legacyLogicalPropertiesTransformer, StyleProvider} from "@ant-design/cssinjs";
import {App, ConfigProvider, theme} from "antd";
import zhCN from "antd/es/locale/zh_CN";
import {useEffect, useState} from "react";
import {createRoot} from "react-dom/client";
import {createGlobalStyle} from "styled-components";
import AppBase from "./AppBase";

const {darkAlgorithm, defaultAlgorithm} = theme;

const GlobalStyle = createGlobalStyle`
  body {
      background-color: #f5f7fa;
      color: #1f1f1f;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
      margin: 0;
      transition: background-color 0.2s, color 0.2s;
  }

  body.dark {
      background-color: #141414;
      color: #dfdfdf;
  }
`;

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

const getCookie = (name: string): string | null => {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
        const val = parts.pop()?.split(';').shift() || null;
        return val ? decodeURIComponent(val) : null;
    }
    return null;
}

const Index = () => {
    const [config] = useState<StaticPlusConfig | null>(loadFromDocument);
    const [isDark, setIsDark] = useState<boolean>(false);
    const [primaryColor, setPrimaryColor] = useState<string>("#1677ff");

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

    useEffect(() => {
        const color = getCookie("Admin-Color-Primary");
        if (color) {
            setPrimaryColor(color);
        }
    }, []);

    const safeConfig = config || {};

    return (
        <ConfigProvider
            locale={zhCN}
            theme={{
                algorithm: isDark ? darkAlgorithm : defaultAlgorithm,
                token: {
                    colorPrimary: primaryColor,
                }
            }}
        >
            <StyleProvider transformers={[legacyLogicalPropertiesTransformer]}>
                <App>
                    <GlobalStyle />
                    <AppBase config={safeConfig} />
                </App>
            </StyleProvider>
        </ConfigProvider>
    );
};

const container = document.getElementById("app");
const root = createRoot(container!);
root.render(<Index/>);
