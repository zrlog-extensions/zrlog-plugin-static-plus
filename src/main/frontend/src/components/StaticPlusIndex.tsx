import {
    Button,
    Card,
    Form,
    Input,
    Select,
    Switch,
    Space,
    Typography,
    Divider,
    message,
    Tooltip,
    Alert,
    Collapse,
} from "antd";
import {
    GithubOutlined,
    SettingOutlined,
    GlobalOutlined,
    SaveOutlined,
    InfoCircleOutlined,
    KeyOutlined,
    BranchesOutlined,
    UserOutlined,
    MailOutlined,
    CloudOutlined,
    ApiOutlined,
} from "@ant-design/icons";
import axios from "axios";
import {FunctionComponent, useState} from "react";
import {StaticPlusConfig} from "../index";

const {Title, Paragraph, Text} = Typography;

type StaticPlusIndexProps = {
    config: StaticPlusConfig;
}

interface GitConfig {
    username?: string;
    password?: string;
    branch?: string;
    url?: string;
    accessBaseUrl?: string;
    gitCommitterUsername?: string;
    gitCommitterEmail?: string;
    proxyHttpHost?: string;
    proxyHttpPort?: number | string;
}

interface FormValues {
    syncRemoteType: string;
    gitUrl: string;
    gitBranch: string;
    gitUsername: string;
    gitPassword?: string;
    gitAccessBaseUrl: string;
    gitCommitterUsername: string;
    gitCommitterEmail: string;
    proxyHttpHost: string;
    proxyHttpPort?: number | string;
    syncTemplate: boolean;
    syncHtml: boolean;
    syncAttached: boolean;
}

const syncRemoteTypeOptions = [
    {label: "Git 仓库 (GitHub / Gitee / GitLab / Coding)", value: "git"},
];

const StaticPlusIndex: FunctionComponent<StaticPlusIndexProps> = ({config}) => {
    const [loading, setLoading] = useState(false);
    const [form] = Form.useForm<FormValues>();
    const [messageApi, contextHolder] = message.useMessage();

    // Parse the inner Git configuration string
    const gitData = (): GitConfig => {
        if (!config.git) {
            return {};
        }
        try {
            return JSON.parse(config.git) as GitConfig;
        } catch (e) {
            console.error("Failed to parse Git config", e);
            return {};
        }
    };

    const git = gitData();

    // Initial form values mapping
    const initialValues: FormValues = {
        syncRemoteType: config.syncRemoteType || "git",
        gitUrl: git.url || "",
        gitBranch: git.branch || "main",
        gitUsername: git.username || "",
        gitPassword: git.password || "",
        gitAccessBaseUrl: git.accessBaseUrl || "",
        gitCommitterUsername: git.gitCommitterUsername || "",
        gitCommitterEmail: git.gitCommitterEmail || "",
        proxyHttpHost: git.proxyHttpHost || "",
        proxyHttpPort: git.proxyHttpPort || "",
        syncTemplate: config.syncTemplate === "on",
        syncHtml: config.syncHtml === "on",
        syncAttached: config.syncAttached === "on",
    };

    const handleSave = async (values: FormValues) => {
        setLoading(true);
        try {
            const gitConfig: GitConfig = {
                url: values.gitUrl.trim(),
                branch: values.gitBranch.trim(),
                username: values.gitUsername.trim(),
                password: values.gitPassword ? values.gitPassword.trim() : "",
                accessBaseUrl: values.gitAccessBaseUrl.trim(),
                gitCommitterUsername: values.gitCommitterUsername ? values.gitCommitterUsername.trim() : "",
                gitCommitterEmail: values.gitCommitterEmail ? values.gitCommitterEmail.trim() : "",
                proxyHttpHost: values.proxyHttpHost ? values.proxyHttpHost.trim() : "",
                proxyHttpPort: values.proxyHttpPort ? Number(values.proxyHttpPort) : "",
            };

            const params = new URLSearchParams();
            params.append("syncRemoteType", values.syncRemoteType);
            params.append("git", JSON.stringify(gitConfig));
            params.append("syncTemplate", values.syncTemplate ? "on" : "off");
            params.append("syncHtml", values.syncHtml ? "on" : "off");
            params.append("syncAttached", values.syncAttached ? "on" : "off");

            const {data} = await axios.post("update", params, {
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                },
            });

            if (data.success || data.status === 200) {
                messageApi.success({
                    content: "配置保存成功！系统将在后台自动生成静态文件并触发同步任务。",
                    duration: 3,
                });
            } else {
                throw new Error(data.message || "请求返回异常");
            }
        } catch (e) {
            messageApi.error({
                content: e instanceof Error ? `保存失败：${e.message}` : "保存失败，请检查网络或后端日志",
                duration: 4,
            });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="static-plus-shell">
            {contextHolder}

            {/* Header section with rich aesthetics */}
            <div className="static-plus-topbar">
                <div className="topbar-left">
                    <div className="logo-badge">
                        <GlobalOutlined className="spinning-logo" />
                    </div>
                    <div>
                        <Title level={2} className="static-plus-title">Static Plus 静态化与同步设置</Title>
                        <Paragraph className="static-plus-subtitle">
                            自动把您的 ZrLog 博客编译为百分之百纯静态的 HTML 网页，并通过 Git 强力推送部署至您的远程静态服务器中。
                        </Paragraph>
                    </div>
                </div>
            </div>

            <Alert
                message="系统提示"
                description="配置保存后系统会自动在后台运行编译同步服务，首次全量同步花费时间可能稍长，可通过博客控制台的同步日志查看进度。"
                type="info"
                showIcon
                closable
                style={{marginBottom: 20, borderRadius: 8}}
            />

            <Form
                form={form}
                layout="vertical"
                initialValues={initialValues}
                onFinish={handleSave}
                requiredMark="optional"
            >
                <div className="main-layout-grid">
                    {/* Left Column: Basic Settings & Sync Controls */}
                    <div className="layout-col-left">
                        <Card
                            title={<span><SettingOutlined /> 基础同步设置</span>}
                            className="premium-card hover-lift"
                            bordered={false}
                        >
                            <Form.Item
                                label="同步远端类型"
                                name="syncRemoteType"
                                rules={[{required: true}]}
                            >
                                <Select options={syncRemoteTypeOptions} size="large" />
                            </Form.Item>

                            <Divider style={{margin: "16px 0"}} />

                            <div className="switch-group">
                                <div className="switch-item">
                                    <div className="switch-info">
                                        <Text className="switch-label">主题静态文件同步</Text>
                                        <Paragraph className="switch-desc">
                                            同步博客当前活跃主题下的 CSS, JS 和其他主题附带的静态资产（需要主题支持）。
                                        </Paragraph>
                                    </div>
                                    <Form.Item name="syncTemplate" valuePropName="checked" noStyle>
                                        <Switch checkedChildren="开启" unCheckedChildren="关闭" />
                                    </Form.Item>
                                </div>

                                <Divider style={{margin: "12px 0"}} />

                                <div className="switch-item">
                                    <div className="switch-info">
                                        <Text className="switch-label">静态缓存 HTML 同步</Text>
                                        <Paragraph className="switch-desc">
                                            全量静态化博客所有页面并同步成 HTML，极大加快前台站点的访问和响应速度。
                                        </Paragraph>
                                    </div>
                                    <Form.Item name="syncHtml" valuePropName="checked" noStyle>
                                        <Switch checkedChildren="开启" unCheckedChildren="关闭" />
                                    </Form.Item>
                                </div>

                                <Divider style={{margin: "12px 0"}} />

                                <div className="switch-item">
                                    <div className="switch-info">
                                        <Text className="switch-label">静态附件同步</Text>
                                        <Paragraph className="switch-desc">
                                            同步随文章一同上传至附件库中的各种媒体资源，如图片、文件等资产。
                                        </Paragraph>
                                    </div>
                                    <Form.Item name="syncAttached" valuePropName="checked" noStyle>
                                        <Switch checkedChildren="开启" unCheckedChildren="关闭" />
                                    </Form.Item>
                                </div>
                            </div>
                        </Card>
                    </div>

                    {/* Right Column: Git Configuration Cards */}
                    <div className="layout-col-right">
                        <Card
                            title={
                                <Space>
                                    <GithubOutlined />
                                    <span>Git 仓库同步凭据</span>
                                </Space>
                            }
                            className="premium-card hover-lift"
                            bordered={false}
                        >
                            <Form.Item
                                label={
                                    <Space>
                                        <span>Git 仓库 URL</span>
                                        <Tooltip title="输入您的托管服务仓库 SSH 或 HTTPS 链接。推荐使用 https 格式配合个人访问令牌（Personal Access Token）。">
                                            <InfoCircleOutlined style={{color: "rgba(0,0,0,0.45)"}} />
                                        </Tooltip>
                                    </Space>
                                }
                                name="gitUrl"
                                rules={[
                                    {required: true, message: "请输入 Git 仓库地址"},
                                    {
                                        pattern: /^(https:\/\/|git@|github\.com)/,
                                        message: "请输入合法的 Git 仓库链接",
                                    },
                                ]}
                            >
                                <Input
                                    prefix={<GlobalOutlined className="input-icon" />}
                                    placeholder="https://github.com/username/username.github.io.git"
                                    size="large"
                                />
                            </Form.Item>

                            <Form.Item
                                label={
                                    <Space>
                                        <span>分支名称 (Branch)</span>
                                        <Tooltip title="要同步和推送的分支，例如 main, master, 或 gh-pages。">
                                            <InfoCircleOutlined style={{color: "rgba(0,0,0,0.45)"}} />
                                        </Tooltip>
                                    </Space>
                                }
                                name="gitBranch"
                                rules={[{required: true, message: "请输入分支名称"}]}
                            >
                                <Input
                                    prefix={<BranchesOutlined className="input-icon" />}
                                    placeholder="例如: main"
                                    size="large"
                                />
                            </Form.Item>

                            <Form.Item
                                label="Git 账号 / 凭证所有者"
                                name="gitUsername"
                                rules={[{required: true, message: "请输入用户名"}]}
                            >
                                <Input
                                    prefix={<UserOutlined className="input-icon" />}
                                    placeholder="输入您的托管平台登录邮箱或用户名"
                                    size="large"
                                />
                            </Form.Item>

                            <Form.Item
                                label={
                                    <Space>
                                        <span>密码 / 个人访问令牌 (Token)</span>
                                        <Tooltip title="对于 GitHub/Gitee，建议使用生成的个人访问令牌 (PAT) 代替明文登录密码，更加安全且不受多因素验证(2FA)影响。">
                                            <InfoCircleOutlined style={{color: "rgba(0,0,0,0.45)"}} />
                                        </Tooltip>
                                    </Space>
                                }
                                name="gitPassword"
                            >
                                <Input.Password
                                    prefix={<KeyOutlined className="input-icon" />}
                                    placeholder="输入账户登录密码或 Token 密钥令牌"
                                    size="large"
                                />
                            </Form.Item>

                            <Form.Item
                                label={
                                    <Space>
                                        <span>自定义静态博客域名</span>
                                        <Tooltip title="访问该静态网站所需的自定义独立域名或 GitHub 默认分配子域名（用作静态页面解析优化）。">
                                            <InfoCircleOutlined style={{color: "rgba(0,0,0,0.45)"}} />
                                        </Tooltip>
                                    </Space>
                                }
                                name="gitAccessBaseUrl"
                            >
                                <Input
                                    prefix={<GlobalOutlined className="input-icon" />}
                                    placeholder="例如: https://username.github.io"
                                    size="large"
                                />
                            </Form.Item>

                            <Collapse ghost style={{ marginTop: 16 }}>
                                <Collapse.Panel
                                    header={<span style={{ fontWeight: 600, color: '#4f46e5' }}>高级 Git 配置 (网络代理与提交人信息)</span>}
                                    key="advanced"
                                >
                                    <div style={{ padding: '0 8px 8px' }}>
                                        <Divider orientation={"left" as any} style={{ margin: '8px 0 16px', fontSize: 13 }}>
                                            <ApiOutlined /> Git 提交者信息
                                        </Divider>
                                        <Form.Item
                                            label="提交人用户名 (Committer Name)"
                                            name="gitCommitterUsername"
                                        >
                                            <Input
                                                prefix={<UserOutlined className="input-icon" />}
                                                placeholder="例如: ZrLog Bot"
                                                size="large"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="提交人邮箱 (Committer Email)"
                                            name="gitCommitterEmail"
                                            rules={[
                                                {
                                                    type: "email",
                                                    message: "请输入合法的邮箱地址",
                                                }
                                            ]}
                                        >
                                            <Input
                                                prefix={<MailOutlined className="input-icon" />}
                                                placeholder="例如: bot@example.com"
                                                size="large"
                                            />
                                        </Form.Item>

                                        <Divider orientation={"left" as any} style={{ margin: '24px 0 16px', fontSize: 13 }}>
                                            <CloudOutlined /> 网络代理设置
                                        </Divider>
                                        <Form.Item
                                            label="HTTP 代理主机 (HTTP Proxy Host)"
                                            name="proxyHttpHost"
                                        >
                                            <Input
                                                prefix={<GlobalOutlined className="input-icon" />}
                                                placeholder="例如: 127.0.0.1"
                                                size="large"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="HTTP 代理端口 (HTTP Proxy Port)"
                                            name="proxyHttpPort"
                                            rules={[
                                                {
                                                    pattern: /^[0-9]*$/,
                                                    message: "请输入有效的端口号",
                                                }
                                            ]}
                                        >
                                            <Input
                                                prefix={<ApiOutlined className="input-icon" />}
                                                placeholder="例如: 7890"
                                                size="large"
                                            />
                                        </Form.Item>
                                    </div>
                                </Collapse.Panel>
                            </Collapse>
                        </Card>
                    </div>
                </div>

                {/* Footer action row */}
                <div className="static-plus-actions-footer">
                    <Button
                        type="primary"
                        htmlType="submit"
                        size="large"
                        icon={<SaveOutlined />}
                        loading={loading}
                        className="btn-submit-premium"
                    >
                        保存配置并运行同步
                    </Button>
                </div>
            </Form>
        </div>
    );
};

export default StaticPlusIndex;
