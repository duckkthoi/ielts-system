import { lazy, Suspense, useState } from "react"
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  ConfigProvider,
  DatePicker,
  Drawer,
  Empty,
  Form,
  Input,
  Modal,
  Pagination,
  Progress,
  Segmented,
  Select,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
} from "antd"
import {
  Bell,
  ChevronRight,
  LoaderCircle,
  Pencil,
  Plus,
  Save,
  Search,
  Trash2,
  UserRound,
} from "lucide-react"
import type { ColumnsType } from "antd/es/table"
import {
  AutosaveIndicator,
  BandBadge,
  ChatMessage,
  CountdownTimer,
  CriterionCard,
  InlineComment,
  PageHeader,
  PresenceDot,
  ProgressCell,
  RowActions,
  ScorePicker,
  StatCard,
  StatusTag,
  WordCounter,
  type SubmissionStatus,
} from "./components/DesignSystem"
import { Prompt2Screens } from "./components/Prompt2Screens"
import { Prompt3Screens } from "./components/Prompt3Screens"

const Prompt4To10Screens = lazy(() =>
  import("./components/Prompt4To10Screens").then((module) => ({
    default: module.Prompt4To10Screens,
  })),
)

const { Text, Title, Paragraph } = Typography

type StudentRow = {
  key: string
  studentId: string
  name: string
  status: SubmissionStatus
  band?: number
}

const students: StudentRow[] = [
  {
    key: "1",
    studentId: "2151000001",
    name: "Nguyễn Văn An",
    status: "SUBMITTED",
  },
  {
    key: "2",
    studentId: "2151000002",
    name: "Trần Thị Bình",
    status: "RETURNED",
    band: 6.5,
  },
  { key: "3", studentId: "2151000003", name: "Lê Hoàng Cường", status: "LATE" },
]

const columns: ColumnsType<StudentRow> = [
  {
    title: "Sinh viên",
    dataIndex: "name",
    render: (name: string, row) => (
      <Space>
        <PresenceDot
          state={
            row.key === "1" ? "online" : row.key === "2" ? "away" : "offline"
          }
        >
          <Avatar>{name.split(" ").at(-1)?.charAt(0)}</Avatar>
        </PresenceDot>
        <div>
          <Text strong>{name}</Text>
          <Text type="secondary" className="block-copy mono-copy">
            {row.studentId}
          </Text>
        </div>
      </Space>
    ),
  },
  {
    title: "Trạng thái",
    dataIndex: "status",
    render: (status: SubmissionStatus) => <StatusTag status={status} />,
  },
  {
    title: "Tiến độ",
    dataIndex: "status",
    align: "center",
    render: (status: SubmissionStatus, row) => (
      <ProgressCell status={status} band={row.band} pulse={row.key === "1"} />
    ),
  },
  {
    title: "Thao tác",
    key: "actions",
    width: 88,
    fixed: "right",
    render: () => <RowActions />,
  },
]

function Section({
  number,
  title,
  description,
  children,
}: {
  number: string
  title: string
  description: string
  children: React.ReactNode
}) {
  return (
    <section className="catalog-section">
      <div className="section-heading">
        <Text className="section-number">{number}</Text>
        <div>
          <Title level={2}>{title}</Title>
          <Paragraph type="secondary">{description}</Paragraph>
        </div>
      </div>
      {children}
    </section>
  )
}

function App() {
  const [workspace, setWorkspace] =
    useState<"components" | "prompt2" | "prompt3" | "remaining">("remaining")
  const [drawerOpen, setDrawerOpen] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [score, setScore] = useState(6)

  return (
    <ConfigProvider
      theme={{
        token: {
          colorPrimary: "var(--brand-600)",
          colorInfo: "var(--info)",
          colorSuccess: "var(--success)",
          colorWarning: "var(--warning)",
          colorError: "var(--error)",
          colorText: "var(--neutral-700)",
          colorTextHeading: "var(--neutral-900)",
          colorTextSecondary: "var(--neutral-500)",
          colorBorder: "var(--neutral-100)",
          colorBgLayout: "var(--neutral-25)",
          borderRadius: 8,
          borderRadiusLG: 12,
          controlHeight: 36,
          fontFamily: "Roboto, sans-serif",
          boxShadow: "var(--shadow-md)",
        },
        components: {
          Button: { primaryShadow: "none", fontWeight: 500 },
          Card: { paddingLG: 24 },
          Table: {
            headerBg: "var(--neutral-50)",
            headerColor: "var(--neutral-700)",
            rowHoverBg: "var(--neutral-25)",
          },
        },
      }}
    >
      <div className="app-shell">
        <header className="catalog-nav">
          <div className="brand-lockup">
            <span className="brand-mark">IW</span>
            <div>
              <Text strong>IELTS Writing</Text>
              <Text type="secondary" className="block-copy">
                Design system
              </Text>
            </div>
          </div>
          <Segmented
            className="workspace-switcher"
            value={workspace}
            options={[
              { value: "components", label: "Components" },
              { value: "prompt2", label: "Prompt 2" },
              { value: "prompt3", label: "Prompt 3" },
              { value: "remaining", label: "Prompt 4–10" },
            ]}
            onChange={(value) =>
              setWorkspace(
                value as "components" | "prompt2" | "prompt3" | "remaining",
              )
            }
          />
          <Space>
            <Tag color="blue">v1.0</Tag>
            <Badge count={3}>
              <Tooltip title="Thông báo">
                <Button type="text" icon={<Bell />} aria-label="Thông báo" />
              </Tooltip>
            </Badge>
            <Avatar icon={<UserRound />} />
          </Space>
        </header>

        <main
          className={`catalog-main ${
            workspace !== "components" ? "is-hidden" : ""
          }`}
        >
          <PageHeader
            title="Components"
            description="Thư viện thành phần dùng chung cho hệ thống giao bài và chấm bài IELTS Writing."
            extra={
              <Button type="primary" icon={<Plus />}>
                Tạo thành phần
              </Button>
            }
          />

          <Card className="foundation-card">
            <div>
              <Text className="eyebrow">NỀN TẢNG THIẾT KẾ</Text>
              <Title level={2}>Bình tĩnh, học thuật và đáng tin cậy</Title>
              <Paragraph type="secondary">
                Mọi thành phần dùng lưới 4px, khoảng trắng rộng, màu trạng thái
                nhất quán và nội dung tiếng Việt.
              </Paragraph>
            </div>
            <div className="token-strip">
              {[
                ["Primary", "token-brand"],
                ["Success", "token-success"],
                ["Warning", "token-warning"],
                ["Error", "token-error"],
                ["Surface", "token-surface"],
              ].map(([label, className]) => (
                <div className="token-item" key={label}>
                  <span className={`token-swatch ${className}`} />
                  <Text>{label}</Text>
                </div>
              ))}
            </div>
          </Card>

          <Section
            number="01"
            title="Hành động & trường nhập"
            description="Các điều khiển cơ bản bám sát cấu trúc Ant Design."
          >
            <div className="component-grid two-columns">
              <Card title="Button & IconButton">
                <Space wrap>
                  <Button type="primary" icon={<Plus />}>
                    Tạo bài tập
                  </Button>
                  <Button icon={<Save />}>Lưu nháp</Button>
                  <Button type="text">Xem chi tiết</Button>
                  <Button danger icon={<Trash2 />}>
                    Xoá
                  </Button>
                  <Button loading>Đang lưu</Button>
                  <Button disabled>Đã khoá</Button>
                  <Tooltip title="Chỉnh sửa">
                    <Button icon={<Pencil />} aria-label="Chỉnh sửa" />
                  </Tooltip>
                </Space>
              </Card>
              <Card title="Input, Select, DatePicker, Textarea">
                <Form layout="vertical">
                  <div className="form-grid">
                    <Form.Item
                      label="Tên lớp"
                      validateStatus="error"
                      help="Vui lòng nhập tên lớp"
                    >
                      <Input prefix={<Search />} placeholder="Nhập tên lớp" />
                    </Form.Item>
                    <Form.Item label="Lớp học">
                      <Select
                        defaultValue="IELTS-K15A"
                        options={[
                          { value: "IELTS-K15A", label: "IELTS K15 – Lớp A" },
                        ]}
                      />
                    </Form.Item>
                    <Form.Item label="Hạn nộp">
                      <DatePicker
                        className="full-width"
                        placeholder="Chọn ngày"
                      />
                    </Form.Item>
                    <Form.Item label="Mô tả">
                      <Input.TextArea placeholder="Nhập mô tả…" rows={2} />
                    </Form.Item>
                  </div>
                </Form>
              </Card>
            </div>
          </Section>

          <Section
            number="02"
            title="Trạng thái & điểm số"
            description="Màu sắc được giữ nguyên xuyên suốt toàn bộ sản phẩm."
          >
            <Card>
              <Space orientation="vertical" size="large" className="full-width">
                <div>
                  <Text strong className="field-label">
                    StatusTag
                  </Text>
                  <Space wrap>
                    {([
                      "NOT_STARTED",
                      "DRAFT",
                      "SUBMITTED",
                      "LATE",
                      "GRADING",
                      "RETURNED",
                      "REVISION_REQUESTED",
                    ] as SubmissionStatus[]).map((status) => (
                      <StatusTag key={status} status={status} />
                    ))}
                    <Tag className="assignment-draft">Nháp</Tag>
                    <Tag className="assignment-open">Đang mở</Tag>
                    <Tag className="assignment-closed">Đã đóng</Tag>
                  </Space>
                </div>
                <div>
                  <Text strong className="field-label">
                    BandBadge
                  </Text>
                  <Space size="middle" align="center" wrap>
                    {[4.5, 5.5, 6.5, 7, 8.5].map((band) => (
                      <BandBadge key={band} band={band} />
                    ))}
                    <BandBadge />
                    <BandBadge band={6.5} size="lg" />
                  </Space>
                </div>
                <div>
                  <Text strong className="field-label">
                    ScorePicker
                  </Text>
                  <Text type="secondary" className="score-demo-label">
                    Mặc định · Band nguyên 0–9
                  </Text>
                  <ScorePicker value={score} onChange={setScore} />
                  <Text type="secondary" className="score-demo-label">
                    Biến thể half-band · chỉ dùng khi giáo viên bật
                  </Text>
                  <ScorePicker
                    value={score}
                    variant="half"
                    onChange={setScore}
                  />
                  <Text type="secondary" className="score-demo-label">
                    Trạng thái giới hạn · tối đa Band 5
                  </Text>
                  <ScorePicker
                    value={5}
                    limit={5}
                    limitReason="Không chia đoạn"
                  />
                  <div className="compact-score">
                    <Text type="secondary">Biến thể gọn</Text>
                    <ScorePicker value={score} compact onChange={setScore} />
                  </div>
                </div>
              </Space>
            </Card>
          </Section>

          <Section
            number="03"
            title="Chấm bài"
            description="Thành phần chuyên biệt cho quy trình đọc, nhận xét và cho điểm."
          >
            <div className="component-grid two-columns grading-grid">
              <CriterionCard completed />
              <Card title="Chỉ báo trong bài viết">
                <Space
                  orientation="vertical"
                  size="large"
                  className="full-width"
                >
                  <Space wrap>
                    <WordCounter current={248} minimum={250} />
                    <WordCounter current={274} minimum={250} />
                  </Space>
                  <Paragraph className="essay-sample">
                    In recent years, many <InlineComment /> interested in
                    whether university education should be free.
                  </Paragraph>
                  <Alert
                    type="info"
                    showIcon
                    title="Nhấn vào phần được đánh dấu để xem nhận xét."
                  />
                </Space>
              </Card>
            </div>
          </Section>

          <Section
            number="04"
            title="Hệ thống & thời gian"
            description="Phản hồi rõ ràng cho tự lưu, kết nối và hạn nộp."
          >
            <div className="component-grid two-columns">
              <Card title="AutosaveIndicator">
                <Space orientation="vertical" size="middle">
                  <AutosaveIndicator state="saved" />
                  <AutosaveIndicator state="saving" />
                  <AutosaveIndicator state="offline" />
                  <AutosaveIndicator state="error" />
                </Space>
              </Card>
              <Card title="CountdownTimer">
                <Space wrap>
                  <CountdownTimer>Còn 2 ngày</CountdownTimer>
                  <CountdownTimer>Còn 05:12:30</CountdownTimer>
                  <CountdownTimer tone="warning">Còn 01:45:10</CountdownTimer>
                  <CountdownTimer tone="error">Còn 00:12:05</CountdownTimer>
                  <CountdownTimer tone="error">Quá hạn 1 giờ</CountdownTimer>
                </Space>
              </Card>
            </div>
            <div className="banner-stack">
              <Alert
                banner
                type="warning"
                showIcon
                title="Mất kết nối… đang thử lại (lần 3)"
              />
              <Alert banner type="success" showIcon title="Đã kết nối lại" />
            </div>
          </Section>

          <Section
            number="05"
            title="Hiện diện & tiến độ"
            description="Tín hiệu realtime cho lớp học và tiến độ nộp bài."
          >
            <Card>
              <div className="presence-progress-layout">
                <div>
                  <Text strong className="field-label">
                    PresenceDot
                  </Text>
                  <Space size="large">
                    {(["online", "away", "offline"] as const).map((state) => (
                      <Space key={state}>
                        <PresenceDot state={state}>
                          <Avatar icon={<UserRound />} />
                        </PresenceDot>
                        <Text>
                          {state === "online"
                            ? "Trực tuyến"
                            : state === "away"
                              ? "Vắng mặt"
                              : "Ngoại tuyến"}
                        </Text>
                      </Space>
                    ))}
                  </Space>
                </div>
                <div>
                  <Text strong className="field-label">
                    ProgressCell
                  </Text>
                  <Space>
                    <ProgressCell status="NOT_STARTED" />
                    <ProgressCell status="DRAFT" />
                    <ProgressCell status="SUBMITTED" pulse />
                    <ProgressCell status="LATE" />
                    <ProgressCell status="RETURNED" band={6.5} />
                  </Space>
                </div>
              </div>
            </Card>
          </Section>

          <Section
            number="06"
            title="Dữ liệu & điều hướng"
            description="Cấu trúc cho dashboard, bảng dữ liệu và tiêu đề trang."
          >
            <div className="stats-grid">
              <StatCard
                label="Bài chờ chấm"
                value="12"
                delta="+12% so với tuần trước"
              />
              <StatCard
                label="Bài tập đang mở"
                value="3"
                delta="+1 bài trong tuần này"
              />
              <StatCard
                label="Sinh viên"
                value="64"
                delta="+8% so với học kỳ trước"
              />
            </div>
            <Card className="table-card">
              <Table
                columns={columns}
                dataSource={students}
                pagination={false}
                scroll={{ x: 720 }}
              />
              <div className="table-footer">
                <Text type="secondary">1–3 trên 125</Text>
                <Pagination
                  size="small"
                  defaultCurrent={1}
                  total={125}
                  pageSize={20}
                  showSizeChanger={false}
                />
              </div>
            </Card>
          </Section>

          <Section
            number="07"
            title="Thông báo"
            description="Chuông, badge và nội dung popover theo thời gian thực."
          >
            <Card>
              <div className="notification-demo">
                <Badge count={3}>
                  <Button size="large" icon={<Bell />}>
                    Thông báo
                  </Button>
                </Badge>
                <div className="notification-panel">
                  <div className="panel-title">
                    <Title level={3}>Thông báo</Title>
                    <Button type="link">Đánh dấu đã đọc tất cả</Button>
                  </div>
                  {[
                    [
                      "Bài tập mới: Tuần 5 – Writing practice",
                      "Hạn nộp 23:59 07/11/2026",
                      "3 phút trước",
                    ],
                    [
                      "Nguyễn Văn An vừa nộp Task 2",
                      "Tuần 5 – Writing practice",
                      "12 phút trước",
                    ],
                    [
                      "Đã có kết quả: Tuần 4 – Task 2",
                      "Band 6.5",
                      "1 giờ trước",
                    ],
                  ].map(([title, body, time], index) => (
                    <div className="notification-item" key={title}>
                      <span
                        className={`unread-dot ${
                          index === 2 ? "read-dot" : ""
                        }`}
                      />
                      <div>
                        <Text strong>{title}</Text>
                        <Text type="secondary" className="block-copy">
                          {body}
                        </Text>
                      </div>
                      <Text type="secondary" className="notification-time">
                        {time}
                      </Text>
                    </div>
                  ))}
                  <Button type="link" block>
                    Xem tất cả <ChevronRight />
                  </Button>
                </div>
              </div>
            </Card>
          </Section>

          <Section
            number="08"
            title="Overlay & trạng thái trống"
            description="Drawer, modal xác nhận và hướng dẫn khi chưa có dữ liệu."
          >
            <div className="component-grid two-columns">
              <Card>
                <Empty
                  image={Empty.PRESENTED_IMAGE_SIMPLE}
                  description="Chưa có lớp nào"
                >
                  <Button type="primary" icon={<Plus />}>
                    Tạo lớp
                  </Button>
                </Empty>
              </Card>
              <Card title="Overlay">
                <Space>
                  <Button type="primary" onClick={() => setDrawerOpen(true)}>
                    Mở Drawer
                  </Button>
                  <Button danger onClick={() => setModalOpen(true)}>
                    Mở Modal
                  </Button>
                </Space>
              </Card>
            </div>
          </Section>

          <Section
            number="09"
            title="Trao đổi"
            description="Tin nhắn, trạng thái gửi và chỉ báo đang nhập."
          >
            <Card className="chat-demo">
              <ChatMessage>Em đã bổ sung ví dụ ở đoạn thứ hai.</ChatMessage>
              <ChatMessage mine>
                Cô đã nhận được. Em kiểm tra thêm phần kết luận nhé.
              </ChatMessage>
              <ChatMessage mine failed>
                Em sẽ sửa ngay ạ.
              </ChatMessage>
              <Text type="secondary" className="typing-copy">
                <LoaderCircle className="spin" /> Cô Hạnh đang gõ…
              </Text>
            </Card>
          </Section>

          <Section
            number="10"
            title="Mẫu bố cục"
            description="Tiêu đề trang, tabs và tiến độ ghép từ các thành phần dùng chung."
          >
            <Card>
              <Tabs
                defaultActiveKey="members"
                items={[
                  {
                    key: "members",
                    label: "Thành viên (32)",
                    children: (
                      <Space orientation="vertical" className="full-width">
                        <Text strong>Tiến độ lớp IELTS K15 – Lớp A</Text>
                        <Progress percent={62} strokeColor="var(--brand-600)" />
                      </Space>
                    ),
                  },
                  {
                    key: "assignments",
                    label: "Bài tập (5)",
                    children: <Empty description="Chưa có dữ liệu mẫu" />,
                  },
                  {
                    key: "statistics",
                    label: "Thống kê",
                    children: <Empty description="Chưa có dữ liệu mẫu" />,
                  },
                ]}
              />
            </Card>
          </Section>
        </main>
        <Prompt2Screens hidden={workspace !== "prompt2"} />
        <Prompt3Screens hidden={workspace !== "prompt3"} />
        {workspace === "remaining" ? (
          <Suspense
            fallback={
              <main className="all-prompts-main">
                <Card>
                  <Text type="secondary">Đang tải thư viện màn hình…</Text>
                </Card>
              </main>
            }
          >
            <Prompt4To10Screens />
          </Suspense>
        ) : null}

        <Drawer
          title="Tạo lớp"
          size={600}
          open={drawerOpen}
          onClose={() => setDrawerOpen(false)}
          extra={<Text type="secondary">Thêm lớp học mới</Text>}
          footer={
            <Space className="drawer-footer">
              <Button onClick={() => setDrawerOpen(false)}>Huỷ</Button>
              <Button type="primary">Lưu</Button>
            </Space>
          }
        >
          <Form layout="vertical">
            <Form.Item label="Mã lớp" extra="Dùng khi import qua admin-cli">
              <Input className="mono-copy" placeholder="IELTS-K15A" />
            </Form.Item>
            <Form.Item label="Tên lớp">
              <Input placeholder="IELTS K15 – Lớp A" />
            </Form.Item>
            <Form.Item label="Mô tả">
              <Input.TextArea rows={4} placeholder="Nhập mô tả lớp học…" />
            </Form.Item>
          </Form>
        </Drawer>
        <Modal
          title="Xoá lớp?"
          open={modalOpen}
          onCancel={() => setModalOpen(false)}
          footer={[
            <Button key="cancel" onClick={() => setModalOpen(false)}>
              Huỷ
            </Button>,
            <Button key="delete" danger type="primary" icon={<Trash2 />}>
              Xoá lớp
            </Button>,
          ]}
        >
          <Paragraph>
            Bạn có chắc muốn xoá lớp IELTS K15 – Lớp A? Thao tác này không thể
            hoàn tác.
          </Paragraph>
        </Modal>
      </div>
    </ConfigProvider>
  )
}

export default App
