import {
    Button,
    Card,
    Form,
    Grid,
    Input,
    Select,
    Switch,
    Space,
    Typography,
    Divider,
    message,
    Tooltip,
    Collapse,
    Table,
    Tag,
    Modal,
} from "antd";
import {
    GithubOutlined,
    SettingOutlined,
    GlobalOutlined,
    CloudServerOutlined,
    InfoCircleOutlined,
    KeyOutlined,
    BranchesOutlined,
    UserOutlined,
    MailOutlined,
    ApiOutlined,
    HistoryOutlined,
    ReloadOutlined,
    CheckCircleOutlined,
    CloseCircleOutlined,
} from "@ant-design/icons";
import axios from "axios";
import {FunctionComponent, useState, useEffect} from "react";
import styled from "styled-components";
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

interface S3Config {
    accessKey?: string;
    secretKey?: string;
    sessionToken?: string;
    endpoint?: string;
    region?: string;
    bucket?: string;
    baseUrl?: string;
    pathStyle?: boolean | string;
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
    s3AccessKey: string;
    s3SecretKey: string;
    s3SessionToken: string;
    s3Endpoint: string;
    s3Region: string;
    s3Bucket: string;
    s3BaseUrl: string;
    s3PathStyle: boolean;
    syncTemplate: boolean;
    syncHtml: boolean;
    syncAttached: boolean;
}

/* Styled Components for Restrained & Professional Layout */
const Shell = styled.div`
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 16px;
  box-sizing: border-box;

  @media (max-width: 1024px) {
      padding: 16px 12px;
  }

  @media (max-width: 575px) {
      padding: 12px;
  }
`;

const Header = styled.div`
  margin-bottom: 20px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;

  h3 {
      margin: 0 !important;
  }

  p {
      margin: 4px 0 0 0 !important;
      font-size: 13px !important;
  }

  body.dark & {
      border-bottom-color: #303030;
  }
`;

const HeaderContent = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;

  @media (max-width: 1024px) {
      flex-direction: column;
      align-items: flex-start;
      gap: 12px;
  }
`;

const FormScrollArea = styled.div`
  max-height: 65vh;
  overflow-y: auto;
  padding-right: 8px;
  overflow-x: hidden;

  /* Custom scrollbar */
  &::-webkit-scrollbar {
      width: 6px;
  }
  &::-webkit-scrollbar-track {
      background: transparent;
  }
  &::-webkit-scrollbar-thumb {
      background: rgba(0, 0, 0, 0.15);
      border-radius: 4px;
  }
  body.dark &::-webkit-scrollbar-thumb {
      background: rgba(255, 255, 255, 0.2);
  }
`;

const MainLayoutGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;

  @media (max-width: 1024px) {
      grid-template-columns: 1fr;
      gap: 12px;
  }
`;

const StyledCard = styled(Card)`
  background: #ffffff !important;
  border: 1px solid #f0f0f0 !important;
  border-radius: 8px !important;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03) !important;

  .ant-card-head {
      border-bottom: 1px solid #f0f0f0 !important;
      padding: 12px 16px !important;
      font-size: 14px;
      font-weight: 600;
  }

  .ant-card-body {
      padding: 16px !important;
      @media (max-width: 1024px) {
          padding: 14px !important;
      }
      @media (max-width: 575px) {
          padding: 12px !important;
      }
  }

  body.dark & {
      background: #1f1f1f !important;
      border-color: #303030 !important;
      box-shadow: none !important;

      .ant-card-head {
          border-color: #303030 !important;
      }
  }
`;

const SwitchGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

const SwitchItem = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;

  @media (max-width: 768px) {
      align-items: flex-start;
  }
`;

const SwitchInfo = styled.div`
  flex: 1;
`;

const SwitchLabel = styled(Text)`
  font-size: 14px;
  font-weight: 600;
`;

const SwitchDesc = styled(Paragraph)`
  margin: 2px 0 0 0 !important;
  font-size: 12px;
  color: #8c8c8c;
  line-height: 1.4;

  body.dark & {
      color: #707070;
  }
`;

const TooltipIcon = styled(InfoCircleOutlined)`
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;

  body.dark & {
      color: rgba(255, 255, 255, 0.45);
  }
`;

const PrefixGlobal = styled(GlobalOutlined)`
  color: rgba(0, 0, 0, 0.35);
  body.dark & {
      color: rgba(255, 255, 255, 0.35);
  }
`;

const PrefixBranches = styled(BranchesOutlined)`
  color: rgba(0, 0, 0, 0.35);
  body.dark & {
      color: rgba(255, 255, 255, 0.35);
  }
`;

const PrefixUser = styled(UserOutlined)`
  color: rgba(0, 0, 0, 0.35);
  body.dark & {
      color: rgba(255, 255, 255, 0.35);
  }
`;

const PrefixKey = styled(KeyOutlined)`
  color: rgba(0, 0, 0, 0.35);
  body.dark & {
      color: rgba(255, 255, 255, 0.35);
  }
`;

const PrefixMail = styled(MailOutlined)`
  color: rgba(0, 0, 0, 0.35);
  body.dark & {
      color: rgba(255, 255, 255, 0.35);
  }
`;

const PrefixApi = styled(ApiOutlined)`
  color: rgba(0, 0, 0, 0.35);
  body.dark & {
      color: rgba(255, 255, 255, 0.35);
  }
`;

const Divider12 = styled(Divider)`
  margin: 12px 0 !important;
`;

const Divider8 = styled(Divider)`
  margin: 8px 0 !important;
`;

const AdvancedCollapse = styled(Collapse)<{ primaryColor?: string }>`
  margin-top: 12px !important;

  .ant-collapse-header {
      font-size: 13px !important;
      color: ${props => props.primaryColor || "#1677ff"} !important;
      font-weight: 500 !important;
      padding: 8px 0 !important;
  }
`;

const AdvancedPanelBody = styled.div`
  padding: 4px 0;
`;

const AdvancedDividerFirst = styled(Divider)`
  margin: 4px 0 12px !important;
  font-size: 12px !important;
`;

const AdvancedDividerSecond = styled(Divider)`
  margin: 16px 0 12px !important;
  font-size: 12px !important;
`;

const HistoryTableText = styled(Text)`
  font-size: 13px;
`;

const HistoryLogText = styled(Text)`
  font-size: 13px;
`;

const syncRemoteTypeOptions = [
    {label: "Git 仓库", value: "git"},
    {label: "S3 协议对象存储", value: "s3"},
];

const StaticPlusIndex: FunctionComponent<StaticPlusIndexProps> = ({config}) => {
    const screens = Grid.useBreakpoint();
    const isPhone = Boolean(screens.xs && !screens.sm);
    const isCompact = !screens.lg;
    const [loading, setLoading] = useState(false);
    const [form] = Form.useForm<FormValues>();
    const [messageApi, contextHolder] = message.useMessage();

    const [historyList, setHistoryList] = useState<any[]>([]);
    const [historyLoading, setHistoryLoading] = useState(false);
    const [settingsVisible, setSettingsVisible] = useState(false);

    const loadHistory = async () => {
        setHistoryLoading(true);
        try {
            const {data} = await axios.get("history");
            if (data.success) {
                try {
                    const parsed = data.data ? JSON.parse(data.data) : [];
                    setHistoryList(parsed);
                } catch (e) {
                    setHistoryList([]);
                }
            }
        } catch (e) {
            console.error("Failed to load history", e);
        } finally {
            setHistoryLoading(false);
        }
    };

    useEffect(() => {
        if (config.syncHistory) {
            try {
                setHistoryList(JSON.parse(config.syncHistory));
            } catch (e) {
                console.error("Failed to parse syncHistory", e);
            }
        } else {
            loadHistory();
        }
    }, [config.syncHistory]);

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

    const s3Data = (): S3Config => {
        if (!config.s3) {
            return {};
        }
        try {
            return JSON.parse(config.s3) as S3Config;
        } catch (e) {
            console.error("Failed to parse S3 config", e);
            return {};
        }
    };

    const s3 = s3Data();
    const syncType = config.syncRemoteType || "git";
    const currentRemoteType = Form.useWatch("syncRemoteType", form) || syncType;
    const parseBoolean = (value: boolean | string | undefined): boolean => {
        if (typeof value === "boolean") {
            return value;
        }
        if (typeof value === "string") {
            return value.trim().toLowerCase() === "true";
        }
        return false;
    };
    const formatSyncDuration = (uploadTimeMs: any) => {
        if (uploadTimeMs === null || uploadTimeMs === undefined || uploadTimeMs === "") {
            return "-";
        }
        const normalized = Number(uploadTimeMs);
        if (!Number.isFinite(normalized) || normalized < 0) {
            return "-";
        }
        if (normalized < 1000) {
            return `${Math.round(normalized)}ms`;
        }
        return `${(normalized / 1000).toFixed(2)}s`;
    };
    const formatSyncType = (syncTypeValue: any) => {
        if (!syncTypeValue || typeof syncTypeValue !== "string") {
            return "未知";
        }
        return syncTypeValue;
    };

    const initialValues: FormValues = {
        syncRemoteType: syncType,
        gitUrl: git.url || "",
        gitBranch: git.branch || "main",
        gitUsername: git.username || "",
        gitPassword: git.password || "",
        gitAccessBaseUrl: git.accessBaseUrl || "",
        gitCommitterUsername: git.gitCommitterUsername || "",
        gitCommitterEmail: git.gitCommitterEmail || "",
        proxyHttpHost: git.proxyHttpHost || "",
        proxyHttpPort: git.proxyHttpPort || "",
        s3AccessKey: s3.accessKey || "",
        s3SecretKey: s3.secretKey || "",
        s3SessionToken: s3.sessionToken || "",
        s3Endpoint: s3.endpoint || "",
        s3Region: s3.region || "",
        s3Bucket: s3.bucket || "",
        s3BaseUrl: s3.baseUrl || "",
        s3PathStyle: parseBoolean(s3.pathStyle),
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
            const s3Config: S3Config = {
                accessKey: values.s3AccessKey ? values.s3AccessKey.trim() : "",
                secretKey: values.s3SecretKey ? values.s3SecretKey.trim() : "",
                sessionToken: values.s3SessionToken ? values.s3SessionToken.trim() : "",
                endpoint: values.s3Endpoint ? values.s3Endpoint.trim() : "",
                region: values.s3Region ? values.s3Region.trim() : "",
                bucket: values.s3Bucket ? values.s3Bucket.trim() : "",
                baseUrl: values.s3BaseUrl ? values.s3BaseUrl.trim() : "",
                pathStyle: values.s3PathStyle || false,
            };

            const params = new URLSearchParams();
            params.append("syncRemoteType", values.syncRemoteType);
            params.append("git", JSON.stringify(gitConfig));
            params.append("s3", JSON.stringify(s3Config));
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
                    content: "配置已保存，并已开始执行同步。",
                    duration: 3,
                });
                setSettingsVisible(false);
                setTimeout(loadHistory, 3000);
            } else {
                throw new Error(data.message || "请求返回异常");
            }
        } catch (e) {
            messageApi.error({
                content: e instanceof Error ? `保存失败：${e.message}` : "保存失败",
                duration: 4,
            });
        } finally {
            setLoading(false);
        }
    };

    return (
        <Shell>
            {contextHolder}

            {/* Restrained and minimal header */}
            <Header>
                <HeaderContent>
                    <div>
                        <Title level={3}>静态同步日志与状态</Title>
                        <Paragraph type="secondary">
                            查看静态资源同步历史。可在“配置同步参数”中修改 Git 凭据和同步范围。
                        </Paragraph>
                    </div>
                    <Space wrap style={{width: isPhone ? "100%" : undefined}}>
                        <Button
                            icon={<ReloadOutlined />}
                            onClick={loadHistory}
                            loading={historyLoading}
                            style={isPhone ? {flex: 1} : undefined}
                        >
                            刷新
                        </Button>
                        <Button
                            type="primary"
                            icon={<SettingOutlined />}
                            onClick={() => setSettingsVisible(true)}
                            style={isPhone ? {flex: 1} : undefined}
                        >
                            配置同步参数
                        </Button>
                    </Space>
                </HeaderContent>
            </Header>

            {/* Mobile Scroll-friendly Sync History Table Card as MAIN display */}
            <StyledCard
                title={
                    <Space>
                        <HistoryOutlined />
                        <span>同步日志记录</span>
                    </Space>
                }
                bordered={false}
            >
                <Table
                    dataSource={historyList}
                    rowKey="id"
                    pagination={{ pageSize: 10 }}
                    size="small"
                    scroll={{ x: 'max-content' }}
                    locale={{ emptyText: "暂无同步记录" }}
                    columns={[
                        {
                            title: "同步时间",
                            dataIndex: "time",
                            key: "time",
                            width: 170,
                            render: (text: string) => <HistoryTableText strong>{text}</HistoryTableText>,
                        },
                        {
                            title: "同步方式",
                            dataIndex: "syncRemoteType",
                            key: "syncRemoteType",
                            width: 130,
                            render: (syncType: any) => <Tag color="blue">{formatSyncType(syncType)}</Tag>,
                        },
                        {
                            title: "状态",
                            dataIndex: "success",
                            key: "success",
                            width: 100,
                            render: (success: boolean) => (
                                success ? 
                                <Tag color="success" icon={<CheckCircleOutlined />}>成功</Tag> : 
                                <Tag color="error" icon={<CloseCircleOutlined />}>失败</Tag>
                            ),
                        },
                        {
                            title: "推送文件",
                            dataIndex: "filesCount",
                            key: "filesCount",
                            width: 100,
                            render: (count: number) => <Tag color="blue">{count} 个</Tag>,
                        },
                        {
                            title: "上传耗时",
                            dataIndex: "uploadTimeMs",
                            key: "uploadTimeMs",
                            width: 120,
                            render: (uploadTimeMs: any) => <HistoryTableText>{formatSyncDuration(uploadTimeMs)}</HistoryTableText>,
                        },
                        {
                            title: "日志详情",
                            dataIndex: "message",
                            key: "message",
                            render: (text: string, record: any) => (
                                <HistoryLogText type={record.success ? "secondary" : "danger"}>{text}</HistoryLogText>
                            ),
                        },
                    ]}
                />
            </StyledCard>

            {/* Modal Dialog for Configuration Settings */}
            <Modal
                title={
                    <Space>
                        <SettingOutlined />
                        <span>配置静态同步参数</span>
                    </Space>
                }
                open={settingsVisible}
                onCancel={() => setSettingsVisible(false)}
                onOk={() => form.submit()}
                confirmLoading={loading}
                width={isPhone ? "calc(100vw - 24px)" : isCompact ? "92vw" : 850}
                okText="保存配置并运行同步"
                cancelText="取消"
                destroyOnClose
            >
                <FormScrollArea>
                    <Form
                        form={form}
                        layout="vertical"
                        initialValues={initialValues}
                        onFinish={handleSave}
                        requiredMark="optional"
                    >
                        <MainLayoutGrid>
                            {/* Left Column: Basic Settings */}
                            <div className="layout-col-left">
                                <StyledCard
                                    title={<span><SettingOutlined /> 基础同步设置</span>}
                                    bordered={false}
                                >
                                    <Form.Item
                                        name="syncRemoteType"
                                        rules={[{required: true}]}
                                    >
                                        <Select options={syncRemoteTypeOptions} />
                                    </Form.Item>

                                    <Divider12 />

                                    <SwitchGroup>
                                        <SwitchItem>
                                            <SwitchInfo>
                                                <SwitchLabel>主题静态文件同步</SwitchLabel>
                                                <SwitchDesc>
                                                    同步活跃主题下的 CSS、JS 和图片静态资源。
                                                </SwitchDesc>
                                            </SwitchInfo>
                                            <Form.Item name="syncTemplate" valuePropName="checked" noStyle>
                                                <Switch size="small" />
                                            </Form.Item>
                                        </SwitchItem>

                                        <Divider8 />

                                        <SwitchItem>
                                            <SwitchInfo>
                                                <SwitchLabel>静态缓存 HTML 同步</SwitchLabel>
                                                <SwitchDesc>
                                                    静态化全站文章与页面 HTML 并同步。
                                                </SwitchDesc>
                                            </SwitchInfo>
                                            <Form.Item name="syncHtml" valuePropName="checked" noStyle>
                                                <Switch size="small" />
                                            </Form.Item>
                                        </SwitchItem>

                                        <Divider8 />

                                        <SwitchItem>
                                            <SwitchInfo>
                                                <SwitchLabel>静态附件同步</SwitchLabel>
                                                <SwitchDesc>
                                                    同步上传到附件库中的媒体资产。
                                                </SwitchDesc>
                                            </SwitchInfo>
                                            <Form.Item name="syncAttached" valuePropName="checked" noStyle>
                                                <Switch size="small" />
                                            </Form.Item>
                                        </SwitchItem>
                                    </SwitchGroup>
                                </StyledCard>
                            </div>

                            {/* Right Column: Git Configuration */}
                            <div className="layout-col-right">
                                {currentRemoteType === "s3" ? (
                                    <StyledCard
                                        title={
                                            <Space>
                                                <CloudServerOutlined />
                                                <span>S3 协议对象存储配置</span>
                                            </Space>
                                        }
                                        bordered={false}
                                    >
                                        <Paragraph type="secondary" style={{marginBottom: 16}}>
                                            兼容标准 S3 API，可直接对接 AWS S3、Cloudflare R2 等对象存储服务。
                                        </Paragraph>

                                        <Form.Item
                                            label="Access Key"
                                            name="s3AccessKey"
                                            rules={[
                                                {
                                                    required: currentRemoteType === "s3",
                                                    message: "请输入 Access Key",
                                                },
                                            ]}
                                        >
                                            <Input
                                                prefix={<PrefixKey />}
                                                placeholder="AKIA... / 对象存储 Access Key"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="Secret Key"
                                            name="s3SecretKey"
                                            rules={[
                                                {
                                                    required: currentRemoteType === "s3",
                                                    message: "请输入 Secret Key",
                                                },
                                            ]}
                                        >
                                            <Input.Password
                                                prefix={<PrefixKey />}
                                                placeholder="对象存储 Secret Key"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="Session Token（可选）"
                                            name="s3SessionToken"
                                        >
                                            <Input
                                                prefix={<PrefixKey />}
                                                placeholder="（可选）临时凭据 Token"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="Endpoint（留空将使用 AWS 标准 endpoint）"
                                            name="s3Endpoint"
                                        >
                                            <Input
                                                prefix={<PrefixGlobal />}
                                                placeholder="例如: https://s3.us-east-1.amazonaws.com"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="Region"
                                            name="s3Region"
                                        >
                                            <Input
                                                prefix={<PrefixGlobal />}
                                                placeholder="例如: us-east-1（留空默认 us-east-1）"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="Bucket"
                                            name="s3Bucket"
                                            rules={[
                                                {
                                                    required: currentRemoteType === "s3",
                                                    message: "请输入 Bucket",
                                                },
                                            ]}
                                        >
                                            <Input
                                                prefix={<PrefixGlobal />}
                                                placeholder="例如: my-static-bucket"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="公共访问域名（可选）"
                                            name="s3BaseUrl"
                                        >
                                            <Input
                                                prefix={<PrefixGlobal />}
                                                placeholder="例如: https://cdn.example.com"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="使用 pathStyle 访问方式"
                                            name="s3PathStyle"
                                            valuePropName="checked"
                                        >
                                            <Switch size="small" />
                                        </Form.Item>
                                    </StyledCard>
                                ) : (
                                    <StyledCard
                                        title={
                                            <Space>
                                                <GithubOutlined />
                                                <span>Git 仓库配置</span>
                                            </Space>
                                        }
                                        bordered={false}
                                    >
                                        <Form.Item
                                            label={
                                                <Space>
                                                    <span>Git 仓库 URL</span>
                                                    <Tooltip title="Git 服务的 SSH 或 HTTPS 链接。HTTPS 方式通常需要配合 Access Token。">
                                                        <TooltipIcon />
                                                    </Tooltip>
                                                </Space>
                                            }
                                            name="gitUrl"
                                            rules={[
                                                {required: currentRemoteType === "git", message: "请输入 Git 仓库地址"},
                                                {
                                                    pattern: /^(https:\/\/|git@|github\.com)/,
                                                    message: "请输入合法的 Git 仓库链接",
                                                },
                                            ]}
                                        >
                                            <Input
                                                prefix={<PrefixGlobal />}
                                                placeholder="https://github.com/username/repo.github.io.git"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="分支名称 (Branch)"
                                            name="gitBranch"
                                            rules={[{required: currentRemoteType === "git", message: "请输入分支名称"}]}
                                        >
                                            <Input
                                                prefix={<PrefixBranches />}
                                                placeholder="例如: main"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="Git 凭证所有者"
                                            name="gitUsername"
                                            rules={[{required: currentRemoteType === "git", message: "请输入用户名"}]}
                                        >
                                            <Input
                                                prefix={<PrefixUser />}
                                                placeholder="输入 Git 服务用户名或邮箱"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label={
                                                <Space>
                                                    <span>密码 / 个人访问令牌 (Token)</span>
                                                    <Tooltip title="对于 GitHub，建议使用生成的个人访问令牌 (PAT) 代替明文登录密码以保障安全。">
                                                        <TooltipIcon />
                                                    </Tooltip>
                                                </Space>
                                            }
                                            name="gitPassword"
                                        >
                                            <Input.Password
                                                prefix={<PrefixKey />}
                                                placeholder="输入账户密码或 Access Token"
                                            />
                                        </Form.Item>

                                        <Form.Item
                                            label="自定义静态域名"
                                            name="gitAccessBaseUrl"
                                        >
                                            <Input
                                                prefix={<PrefixGlobal />}
                                                placeholder="例如: https://username.github.io"
                                            />
                                        </Form.Item>

                                        <AdvancedCollapse ghost primaryColor={config.adminColorPrimary}>
                                            <Collapse.Panel
                                                header="高级配置 (代理与提交人)"
                                                key="advanced"
                                            >
                                                <AdvancedPanelBody>
                                                    <AdvancedDividerFirst orientation={"left" as any}>
                                                        提交者身份
                                                    </AdvancedDividerFirst>
                                                    <Form.Item
                                                        label="提交人用户名"
                                                        name="gitCommitterUsername"
                                                    >
                                                        <Input
                                                            prefix={<PrefixUser />}
                                                            placeholder="例如: ZrLog Bot"
                                                        />
                                                    </Form.Item>

                                                    <Form.Item
                                                        label="提交人邮箱"
                                                        name="gitCommitterEmail"
                                                        rules={[
                                                            {
                                                                type: "email",
                                                                message: "请输入合法的邮箱地址",
                                                            },
                                                        ]}
                                                    >
                                                        <Input
                                                            prefix={<PrefixMail />}
                                                            placeholder="例如: bot@example.com"
                                                        />
                                                    </Form.Item>

                                                    <AdvancedDividerSecond orientation={"left" as any}>
                                                        网络代理
                                                    </AdvancedDividerSecond>
                                                    <Form.Item
                                                        label="HTTP 代理主机"
                                                        name="proxyHttpHost"
                                                    >
                                                        <Input
                                                            prefix={<PrefixGlobal />}
                                                            placeholder="例如: 127.0.0.1"
                                                        />
                                                    </Form.Item>

                                                    <Form.Item
                                                        label="HTTP 代理端口"
                                                        name="proxyHttpPort"
                                                        rules={[
                                                            {
                                                                pattern: /^[0-9]*$/,
                                                                message: "请输入有效的端口号",
                                                            },
                                                        ]}
                                                    >
                                                        <Input
                                                            prefix={<PrefixApi />}
                                                            placeholder="例如: 7890"
                                                        />
                                                    </Form.Item>
                                                </AdvancedPanelBody>
                                            </Collapse.Panel>
                                        </AdvancedCollapse>
                                    </StyledCard>
                                )}
                            </div>
                        </MainLayoutGrid>
                    </Form>
                </FormScrollArea>
            </Modal>
        </Shell>
    );
};

export default StaticPlusIndex;
