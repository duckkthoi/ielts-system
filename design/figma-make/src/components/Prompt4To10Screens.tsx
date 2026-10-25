import { useState } from "react"
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Checkbox,
  Collapse,
  DatePicker,
  Drawer,
  Dropdown,
  Empty,
  Form,
  Input,
  InputNumber,
  Modal,
  Popover,
  Progress,
  Radio,
  Segmented,
  Select,
  Skeleton,
  Space,
  Steps,
  Switch,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  Upload,
} from "antd"
import {
  ArrowLeft,
  BarChart3,
  Bell,
  BookOpen,
  Check,
  ChevronLeft,
  ChevronRight,
  ClipboardCheck,
  Copy,
  Download,
  FileText,
  GripVertical,
  History,
  LayoutDashboard,
  ListChecks,
  MessageCircle,
  MoreHorizontal,
  Paperclip,
  Pencil,
  Plus,
  Search,
  Send,
  Sparkles,
  Trash2,
  UserCog,
  Users,
  WifiOff,
} from "lucide-react"
import {
  AutosaveIndicator,
  BandBadge,
  ChatMessage,
  ChecklistItem,
  CommentBankPopover,
  CountdownTimer,
  CriterionCard,
  DescriptorPanel,
  ErrorCodePicker,
  ErrorSummary,
  InlineComment,
  PageHeader,
  PresenceDot,
  ProgressCell,
  ScorePicker,
  StatCard,
  StatusTag,
  WordCounter,
  type SubmissionStatus,
} from "./DesignSystem"

const { Text, Title, Paragraph } = Typography
type PromptKey = "4" | "5" | "6" | "7" | "8" | "9" | "10"

const students = [
  ["2151000001", "Nguyễn Văn An", "online"],
  ["2151000002", "Trần Thị Bình", "away"],
  ["2151000003", "Lê Hoàng Cường", "offline"],
  ["2151000004", "Phạm Thu Dung", "online"],
  ["2151000005", "Võ Minh Đức", "offline"],
  ["2151000006", "Đặng Khánh Linh", "online"],
] as const

function PromptToolbar({
  prompt,
  setPrompt,
}: {
  prompt: PromptKey
  setPrompt: (value: PromptKey) => void
}) {
  return (
    <Card className="all-prompts-toolbar">
      <div>
        <Text className="eyebrow">PROMPT 4–10</Text>
        <Title level={2}>Thư viện màn hình nghiệp vụ</Title>
        <Text type="secondary">
          Bao gồm bổ sung nghiệp vụ IELTS chính thức trong Prompt 6b.
        </Text>
      </div>
      <Segmented
        value={prompt}
        options={[
          { value: "4", label: "4 · Bài tập" },
          { value: "5", label: "5 · Tiến độ" },
          { value: "6", label: "6 · Chấm bài" },
          { value: "7", label: "7 · Sinh viên" },
          { value: "8", label: "8 · Dashboard" },
          { value: "9", label: "9 · Admin" },
          { value: "10", label: "10 · States" },
        ]}
        onChange={(value) => setPrompt(value as PromptKey)}
      />
    </Card>
  )
}

function Shell({
  active,
  children,
  admin = false,
}: {
  active: string
  children: React.ReactNode
  admin?: boolean
}) {
  const items = admin
    ? [
        ["Tổng quan", <LayoutDashboard />],
        ["Giáo viên", <Users />],
        ["Người dùng", <UserCog />],
        ["Nhật ký hệ thống", <FileText />],
        ["Phiên đăng nhập", <History />],
      ]
    : [
        ["Tổng quan", <LayoutDashboard />],
        ["Lớp học", <Users />],
        ["Bài tập", <BookOpen />],
        ["Chấm bài", <ClipboardCheck />],
        ["Ngân hàng đề", <FileText />],
        ["Thống kê", <BarChart3 />],
        ["Trao đổi", <MessageCircle />],
      ]
  return (
    <div className="p4-shell">
      <aside className="p4-sidebar">
        <div className="p4-brand">
          <span className="brand-mark">IW</span>
          <Text strong>IELTS Writing</Text>
        </div>
        <nav>
          {items.map(([label, icon]) => (
            <Button
              type="text"
              icon={icon}
              key={String(label)}
              className={label === active ? "p4-nav-active" : ""}
            >
              {label}
              {label === "Chấm bài" ? <Badge count={12} /> : null}
            </Button>
          ))}
        </nav>
      </aside>
      <div className="p4-shell-main">
        <header className="p4-header">
          <div>
            <Text type="secondary">
              {admin ? "Quản trị viên" : "Giáo viên"}
            </Text>
            <Text strong className="block-copy">
              {active}
            </Text>
          </div>
          <Space>
            <span className="p3-online-dot" />
            <Text type="secondary">Trực tuyến</Text>
            <Badge count={3}>
              <Button type="text" icon={<Bell />} aria-label="Thông báo" />
            </Badge>
            <Avatar>H</Avatar>
          </Space>
        </header>
        <div className="p4-page">{children}</div>
      </div>
    </div>
  )
}

function AssignmentList({ create }: { create: () => void }) {
  const rows = [
    ["Tuần 5 – Writing practice", "Đang mở", "41/64", "12"],
    ["Tuần 4 – Academic writing", "Đã đóng", "32/32", "0"],
    ["Tuần 6 – Process diagram", "Nháp", "0/64", "0"],
  ].map((item, index) => ({ key: index, item }))
  return (
    <>
      <PageHeader
        title="Bài tập"
        description="Tạo, phát hành và theo dõi bài tập của các lớp."
        extra={
          <Button type="primary" icon={<Plus />} onClick={create}>
            Tạo bài tập
          </Button>
        }
      />
      <Card>
        <div className="filter-row">
          <Segmented options={["Tất cả", "Nháp", "Đang mở", "Đã đóng"]} />
          <Input prefix={<Search />} placeholder="Tìm bài tập" />
        </div>
        <Table
          pagination={false}
          scroll={{ x: 1000 }}
          dataSource={rows}
          columns={[
            {
              title: "Tên bài tập",
              render: (_, row) => (
                <div>
                  <Text strong>{row.item[0]}</Text>
                  <Text type="secondary" className="block-copy">
                    2 phần · Task 1, Task 2
                  </Text>
                </div>
              ),
            },
            {
              title: "Trạng thái",
              render: (_, row) => (
                <Tag
                  className={
                    row.item[1] === "Đang mở"
                      ? "assignment-open"
                      : row.item[1] === "Nháp"
                        ? "assignment-draft"
                        : "assignment-closed"
                  }
                >
                  {row.item[1]}
                </Tag>
              ),
            },
            {
              title: "Giao cho",
              render: () => "IELTS-K15A, IELTS-K15B · 64 SV",
            },
            {
              title: "Hạn nộp",
              render: () => (
                <div>
                  <Text>23:59 07/11/2026</Text>
                  <CountdownTimer tone="warning">Còn 2 ngày</CountdownTimer>
                </div>
              ),
            },
            {
              title: "Đã nộp",
              render: (_, row) => (
                <Space>
                  <Progress
                    percent={Number(row.item[2].split("/")[0])}
                    showInfo={false}
                    size="small"
                  />
                  <Text>{row.item[2]}</Text>
                </Space>
              ),
            },
            {
              title: "Chờ chấm",
              align: "center",
              render: (_, row) => (
                <Badge count={Number(row.item[3])} showZero />
              ),
            },
            {
              title: "",
              render: () => (
                <Space>
                  <Button size="small">Tiến độ</Button>
                  <Button size="small">Chấm bài</Button>
                  <Dropdown
                    menu={{
                      items: [
                        { key: "edit", label: "Sửa" },
                        { key: "copy", label: "Nhân bản" },
                        { key: "close", label: "Đóng bài" },
                        { key: "delete", label: "Xoá", danger: true },
                      ],
                    }}
                  >
                    <Button
                      type="text"
                      icon={<MoreHorizontal />}
                      aria-label="Thêm"
                    />
                  </Dropdown>
                </Space>
              ),
            },
          ]}
        />
      </Card>
    </>
  )
}

function PartCard({ task }: { task: 1 | 2 }) {
  return (
    <Card className="part-card">
      <div className="part-heading">
        <Space>
          <GripVertical />
          <Text strong>Phần {task}</Text>
        </Space>
        <Button type="text" danger icon={<Trash2 />} aria-label="Xoá phần" />
      </div>
      <div className="form-two-columns">
        <Form.Item label="Tiêu đề phần">
          <Input
            defaultValue={
              task === 1 ? "Task 1 – Line graph" : "Task 2 – Opinion essay"
            }
          />
        </Form.Item>
        <Form.Item label="Loại">
          <Select
            defaultValue={`task${task}`}
            options={[
              { value: "task1", label: "IELTS Writing Task 1" },
              { value: "task2", label: "IELTS Writing Task 2" },
              { value: "free", label: "Tự do" },
            ]}
          />
        </Form.Item>
      </div>
      <Form.Item label="Đề bài">
        <Input.TextArea
          rows={4}
          defaultValue={
            task === 1
              ? "The graph below shows the number of international students enrolled in universities in three countries between 2000 and 2020."
              : "Some people believe that university education should be free for everyone. To what extent do you agree or disagree?"
          }
        />
      </Form.Item>
      {task === 1 ? (
        <Form.Item label="Ảnh biểu đồ">
          <Upload.Dragger
            accept="image/*"
            maxCount={1}
            beforeUpload={() => false}
            showUploadList={false}
          >
            <FileText className="part-upload-icon" />
            <Text className="block-copy">
              Kéo ảnh vào đây hoặc bấm để tải lên
            </Text>
            <Text type="secondary">PNG, JPG · tối đa 10 MB</Text>
          </Upload.Dragger>
        </Form.Item>
      ) : null}
      <div className="form-three-columns">
        <Form.Item label="Số từ tối thiểu">
          <InputNumber defaultValue={task === 1 ? 150 : 250} />
        </Form.Item>
        <Form.Item label="Thời gian gợi ý">
          <InputNumber defaultValue={task === 1 ? 20 : 40} />
        </Form.Item>
        <Form.Item label="Hạn riêng">
          <DatePicker />
        </Form.Item>
      </div>
    </Card>
  )
}

function Wizard({ close }: { close: () => void }) {
  const [step, setStep] = useState(0)
  const [publish, setPublish] = useState(false)
  const criteria = [
    "Task Response",
    "Coherence and Cohesion",
    "Lexical Resource",
    "Grammatical Range and Accuracy",
  ]
  return (
    <div className="wizard">
      <div className="wizard-title">
        <Button type="text" icon={<ArrowLeft />} onClick={close}>
          Quay lại
        </Button>
        <Title level={1}>Tạo bài tập</Title>
      </div>
      <Card>
        <Steps
          current={step}
          items={[
            { title: "Thông tin chung" },
            { title: "Các phần" },
            { title: "Tiêu chí & checklist" },
            { title: "Giao bài" },
          ]}
        />
      </Card>
      <div className="wizard-content">
        {step === 0 ? (
          <Card title="Thông tin chung">
            <Form layout="vertical">
              <Form.Item label="Tên bài tập">
                <Input defaultValue="Tuần 5 – Writing practice" />
              </Form.Item>
              <Form.Item label="Mô tả">
                <Input.TextArea rows={4} />
              </Form.Item>
              <div className="form-two-columns">
                <Form.Item
                  label="Thời điểm mở"
                  extra="Để trống = mở ngay khi phát hành"
                >
                  <DatePicker showTime />
                </Form.Item>
                <Form.Item label="Hạn nộp">
                  <DatePicker showTime />
                </Form.Item>
              </div>
              <Form.Item label="Cho phép nộp muộn">
                <Switch defaultChecked />
              </Form.Item>
              <Form.Item label="Số lần yêu cầu sửa tối đa">
                <InputNumber defaultValue={2} />
              </Form.Item>
            </Form>
          </Card>
        ) : null}
        {step === 1 ? (
          <Space orientation="vertical" className="full-width">
            <PartCard task={1} />
            <PartCard task={2} />
            <Button type="dashed" block icon={<Plus />}>
              Thêm phần
            </Button>
          </Space>
        ) : null}
        {step === 2 ? (
          <Card
            title="Tiêu chí & checklist"
            extra={<Button icon={<Download />}>Nạp rubric mẫu IELTS</Button>}
          >
            <Collapse
              defaultActiveKey={["Task Response"]}
              items={criteria.map((name) => ({
                key: name,
                label: name,
                extra: <Tag>Điểm tối đa 9 · Bước 0.5</Tag>,
                children: (
                  <Space orientation="vertical" className="full-width">
                    <Input defaultValue="Trả lời đầy đủ tất cả các phần của đề" />
                    <Input defaultValue="Quan điểm rõ ràng và nhất quán" />
                    <Input defaultValue="Ý chính được phát triển và có ví dụ" />
                    <Button type="dashed" icon={<Plus />}>
                      Thêm mục
                    </Button>
                  </Space>
                ),
              }))}
            />
          </Card>
        ) : null}
        {step === 3 ? (
          <div className="assign-grid">
            <Card title="Giao bài">
              <Checkbox defaultChecked>
                IELTS K15 – Lớp A · 32 sinh viên
              </Checkbox>
              <Checkbox>IELTS K15 – Lớp B · 32 sinh viên</Checkbox>
              <Title level={3}>Giao cho từng sinh viên</Title>
              <Select
                mode="multiple"
                className="full-width"
                placeholder="Tìm theo mã SV hoặc tên"
                defaultValue={["2151000001"]}
                options={students.map(([id, name]) => ({
                  value: id,
                  label: `${id} · ${name}`,
                }))}
              />
            </Card>
            <Card title="Tóm tắt">
              <Title level={2}>33 sinh viên</Title>
              <Text type="secondary">Đã loại trùng · 2 phần</Text>
              <Paragraph>Hạn nộp 23:59 07/11/2026</Paragraph>
              <Button type="primary" block onClick={() => setPublish(true)}>
                Phát hành
              </Button>
            </Card>
          </div>
        ) : null}
      </div>
      <div className="wizard-footer">
        <Button>Lưu nháp</Button>
        <Space>
          <Button disabled={step === 0} onClick={() => setStep(step - 1)}>
            Quay lại
          </Button>
          <Button
            type="primary"
            onClick={() => (step === 3 ? setPublish(true) : setStep(step + 1))}
          >
            {step === 3 ? "Phát hành" : "Tiếp tục"}
          </Button>
        </Space>
      </div>
      <Modal
        title="Phát hành bài tập?"
        open={publish}
        onCancel={() => setPublish(false)}
        okText="Phát hành"
        cancelText="Huỷ"
      >
        <Paragraph>33 sinh viên sẽ nhận thông báo và email.</Paragraph>
      </Modal>
    </div>
  )
}

function Prompt4() {
  const [wizard, setWizard] = useState(false)
  return (
    <Shell active="Bài tập">
      {wizard ? (
        <Wizard close={() => setWizard(false)} />
      ) : (
        <AssignmentList create={() => setWizard(true)} />
      )}
    </Shell>
  )
}

function ProgressScreen({ onRemind }: { onRemind: () => void }) {
  const statuses: SubmissionStatus[] = [
    "SUBMITTED",
    "RETURNED",
    "DRAFT",
    "LATE",
    "GRADING",
    "NOT_STARTED",
  ]
  return (
    <>
      <PageHeader
        title="Tiến độ – Tuần 5 – Writing practice"
        description="Theo dõi tình trạng làm bài theo thời gian thực."
        extra={<Tag color="green">● Cập nhật trực tiếp</Tag>}
      />
      <div className="stats-grid four-stats">
        <StatCard label="Hoàn thành" value="62%" delta="+12% tuần này" />
        <StatCard label="Đã nộp" value="41/64" delta="+8 hôm nay" />
        <StatCard label="Nộp muộn" value="4" delta="6% tổng số" />
        <StatCard label="Đang online" value="9" delta="Trực tiếp" />
      </div>
      <Card>
        <div className="filter-row">
          <Space>
            <Select
              defaultValue="all"
              options={[{ value: "all", label: "Tất cả lớp" }]}
            />
            <Select
              defaultValue="all"
              options={[{ value: "all", label: "Tất cả trạng thái" }]}
            />
            <Input prefix={<Search />} placeholder="Tìm sinh viên" />
          </Space>
          <Space>
            <Button icon={<Bell />} onClick={onRemind}>
              Nhắc nhở (23)
            </Button>
            <Button icon={<Download />}>Xuất Excel</Button>
          </Space>
        </div>
        <Table
          pagination={false}
          dataSource={students.map((item, index) => ({
            key: item[0],
            item,
            index,
          }))}
          columns={[
            { title: "", render: () => <Checkbox /> },
            {
              title: "Sinh viên",
              render: (_, row) => (
                <Space>
                  <PresenceDot state={row.item[2]}>
                    <Avatar size="small">{row.item[1].charAt(0)}</Avatar>
                  </PresenceDot>
                  <div>
                    <Text strong>{row.item[1]}</Text>
                    <Text type="secondary" className="block-copy mono-copy">
                      {row.item[0]}
                    </Text>
                  </div>
                </Space>
              ),
            },
            {
              title: "Task 1 · 28/32",
              align: "center",
              render: (_, row) => (
                <ProgressCell
                  status={statuses[row.index]}
                  pulse={row.index === 0}
                />
              ),
            },
            {
              title: "Task 2 · 23/32",
              align: "center",
              render: (_, row) => (
                <ProgressCell
                  status={statuses[(row.index + 2) % statuses.length]}
                  band={row.index === 1 ? 6.5 : undefined}
                />
              ),
            },
            {
              title: "Band",
              render: (_, row) => (
                <BandBadge
                  band={row.index < 4 ? 5.5 + row.index * 0.5 : undefined}
                />
              ),
            },
          ]}
        />
        <div className="matrix-legend">
          {statuses.map((status) => (
            <StatusTag status={status} size="sm" key={status} />
          ))}
        </div>
      </Card>
    </>
  )
}

function Bars() {
  const data = [
    ["Task Response", 61],
    ["Coherence and Cohesion", 58],
    ["Lexical Resource", 63],
    ["Grammatical Range and Accuracy", 59],
  ]
  return (
    <div className="criteria-bars">
      {data.map(([label, value]) => (
        <div key={label}>
          <Text>{label}</Text>
          <Progress
            percent={Number(value)}
            format={() => (Number(value) / 10).toFixed(1)}
          />
        </div>
      ))}
    </div>
  )
}

function StatsScreen() {
  return (
    <>
      <PageHeader
        title="Thống kê"
        description="Phân tích kết quả theo band và tiêu chí."
        extra={
          <Space>
            <BandBadge band={6} size="lg" />
            <Text strong>Band trung bình 6.0</Text>
          </Space>
        }
      />
      <div className="analytics-grid">
        <Card title="Phân bố Band">
          <div className="bar-chart">
            {[30, 48, 70, 88, 64, 42, 24, 12].map((value, index) => (
              <span key={index} data-value={value}>
                <i />
                <Text>{(4.5 + index * 0.5).toFixed(1)}</Text>
              </span>
            ))}
          </div>
        </Card>
        <Card title="Đúng hạn / Muộn">
          <div className="donut-chart">
            <Text strong>90%</Text>
          </div>
          <Text>Đúng hạn 90% · Muộn 10%</Text>
        </Card>
        <Card title="Điểm trung bình theo tiêu chí">
          <Bars />
        </Card>
        <Card title="Cần hỗ trợ">
          {students.slice(0, 4).map(([id, name], index) => (
            <div className="rank-row" key={id}>
              <Text>{name}</Text>
              <BandBadge band={5 + index * 0.5} size="sm" />
            </div>
          ))}
        </Card>
      </div>
    </>
  )
}

function Prompt5() {
  const [tab, setTab] = useState("progress")
  const [remind, setRemind] = useState(false)
  return (
    <Shell active="Bài tập">
      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={[
          { key: "progress", label: "Tiến độ" },
          { key: "stats", label: "Thống kê" },
        ]}
      />
      {tab === "progress" ? (
        <ProgressScreen onRemind={() => setRemind(true)} />
      ) : (
        <StatsScreen />
      )}
      <Modal
        title="Gửi nhắc nhở cho 23 sinh viên chưa nộp?"
        open={remind}
        onCancel={() => setRemind(false)}
      >
        <Input.TextArea placeholder="Lời nhắn tuỳ chọn" />
      </Modal>
    </Shell>
  )
}

function Queue({ grade }: { grade: () => void }) {
  return (
    <Shell active="Chấm bài">
      <PageHeader
        title="Chấm bài"
        description="Xử lý bài nộp theo thứ tự ưu tiên."
      />
      <Card>
        <div className="queue-filters">
          <Select placeholder="Bài tập" />
          <Select placeholder="Phần" />
          <Select placeholder="Lớp" />
        </div>
        <Segmented
          options={[
            "Chờ chấm (12)",
            "Đang chấm (3)",
            "Đã trả (40)",
            "Cần sửa (5)",
          ]}
        />
        <Table
          pagination={false}
          dataSource={students.slice(0, 5).map((item, index) => ({
            key: item[0],
            item,
            index,
          }))}
          columns={[
            {
              title: "Sinh viên",
              render: (_, row) => <Text strong>{row.item[1]}</Text>,
            },
            {
              title: "Bài tập / Phần",
              render: () => (
                <div>
                  <Text>Tuần 5 – Writing practice</Text>
                  <Text type="secondary" className="block-copy">
                    Task 2 – Opinion essay
                  </Text>
                </div>
              ),
            },
            {
              title: "Version",
              render: (_, row) => <Tag>v{row.index === 0 ? 2 : 1}</Tag>,
            },
            {
              title: "Nộp lúc",
              render: (_, row) => (
                <Space orientation="vertical">
                  <Text>23:41 06/11/2026</Text>
                  {row.index === 0 ? (
                    <StatusTag status="LATE" size="sm" />
                  ) : null}
                </Space>
              ),
            },
            { title: "Số từ", render: () => "274" },
            {
              title: "Trạng thái",
              render: () => <StatusTag status="SUBMITTED" />,
            },
            {
              title: "",
              render: () => (
                <Button type="primary" onClick={grade}>
                  Chấm
                </Button>
              ),
            },
          ]}
        />
      </Card>
    </Shell>
  )
}

function CriterionCommentEditor({
  placeholder = "Nhận xét cho tiêu chí này…",
}: {
  placeholder?: string
}) {
  const [comment, setComment] = useState("")
  return (
    <div className="criterion-comment-editor">
      <Input.TextArea
        rows={3}
        value={comment}
        onChange={(event) => setComment(event.target.value)}
        placeholder={placeholder}
      />
      <CommentBankPopover onInsert={setComment} />
    </div>
  )
}

function LimitedCriterion() {
  const [scale, setScale] = useState<"whole" | "half">("whole")
  return (
    <Card>
      <div className="criterion-row">
        <div>
          <Space>
            <Text strong>Coherence and Cohesion</Text>
            <DescriptorPanel
              criterion="Coherence and Cohesion"
              currentScore={5}
            />
          </Space>
          <Text type="secondary" className="block-copy">
            Tổ chức đoạn và liên kết ý.
          </Text>
        </div>
        <BandBadge band={5} />
      </div>
      <ChecklistItem checked>Mỗi đoạn có câu chủ đề rõ ràng</ChecklistItem>
      <ChecklistItem>Từ nối đa dạng, dùng chính xác</ChecklistItem>
      <ChecklistItem kind="limit" limit={5} checked>
        Không chia đoạn / chia đoạn không đạt
      </ChecklistItem>
      <div className="score-scale-control">
        <Text type="secondary">Thang điểm</Text>
        <Segmented
          size="small"
          value={scale}
          options={[
            { value: "whole", label: "Band nguyên" },
            { value: "half", label: "Nửa band" },
          ]}
          onChange={(value) => setScale(value as "whole" | "half")}
        />
      </div>
      <ScorePicker
        value={5}
        variant={scale}
        limit={5}
        limitReason="Không chia đoạn"
      />
      <CriterionCommentEditor />
    </Card>
  )
}

function Grading({ back }: { back: () => void }) {
  const [revision, setRevision] = useState(false)
  const [ai, setAi] = useState(false)
  const [zero, setZero] = useState(false)
  const [chat, setChat] = useState(false)
  const [zeroReason, setZeroReason] = useState<string>()
  const [generalComment, setGeneralComment] = useState("")
  return (
    <div className="grading-screen">
      <header className="grading-header">
        <Space>
          <Button type="text" icon={<ArrowLeft />} onClick={back}>
            Quay lại
          </Button>
          <div>
            <Text strong>Nguyễn Văn An · 2151000001</Text>
            <Text type="secondary" className="block-copy">
              Tuần 5 · Task 2 – Opinion essay
            </Text>
          </div>
        </Space>
        <Space>
          <Select
            defaultValue="v2"
            options={[{ value: "v2", label: "Version 2 (mới nhất)" }]}
          />
          <StatusTag status="LATE" />
          <Button icon={<ChevronLeft />}>Trước</Button>
          <Button>
            {" "}
            Sau <ChevronRight />
          </Button>
          <Text>3/12</Text>
        </Space>
      </header>
      <div className="grading-layout">
        <section className="essay-panel">
          <Alert
            type="error"
            showIcon
            title="Bài có 18 từ (≤ 20) – mọi tiêu chí tối đa Band 1"
          />
          <Alert
            type="warning"
            showIcon
            title="236 / 250 từ – dưới số từ tối thiểu"
          />
          <Collapse
            defaultActiveKey={["prompt"]}
            items={[
              {
                key: "prompt",
                label: "Đề bài · 274 từ · nộp lúc 23:41 06/11/2026",
                children: (
                  <Paragraph>
                    Some people believe that university education should be free
                    for everyone. To what extent do you agree or disagree?
                  </Paragraph>
                ),
              },
            ]}
          />
          <div className="essay-sheet">
            <Popover open placement="bottom" content={<ErrorCodePicker />}>
              <Paragraph className="essay-copy">
                In recent years, the question of whether higher education should
                be provided free of charge has become a controversial issue.
              </Paragraph>
            </Popover>
            <Paragraph className="essay-copy">
              Firstly, free university education can ensure equal access.
              However,{" "}
              <span className="coded-error">
                people is<sup>VT</sup>
              </span>{" "}
              often prevented from pursuing a degree.
            </Paragraph>
            <div className="inline-suggestion">
              <Text delete>people is</Text>
              <ChevronRight />
              <Text strong>people are</Text>
            </div>
            <Paragraph className="essay-copy">
              This is <InlineComment /> reason governments should invest in
              education.
            </Paragraph>
          </div>
        </section>
        <aside className="grading-panel">
          <div className="grading-title">
            <Title level={2}>Chấm điểm</Title>
            <Space>
              <Button type="text" danger onClick={() => setZero(true)}>
                Chấm 0
              </Button>
              <Button
                type="text"
                icon={<Sparkles />}
                onClick={() => setAi(true)}
              >
                Gợi ý AI
              </Button>
              <Button type="text" icon={<History />}>
                Lịch sử
              </Button>
            </Space>
          </div>
          <Alert
            type="warning"
            showIcon
            title="Bài này giống 78% với bài của Trần Thị Bình"
          />
          <CriterionCard completed />
          <LimitedCriterion />
          <Collapse
            items={[
              {
                key: "lr",
                label: (
                  <Space size={4}>
                    <Text>Lexical Resource · 6</Text>
                    <DescriptorPanel
                      criterion="Lexical Resource"
                      currentScore={6}
                    />
                  </Space>
                ),
                children: (
                  <Space orientation="vertical" className="full-width">
                    <ScorePicker value={6} compact />
                    <CriterionCommentEditor />
                  </Space>
                ),
              },
              {
                key: "gra",
                label: (
                  <Space size={4}>
                    <Text>Grammatical Range and Accuracy · 6</Text>
                    <DescriptorPanel
                      criterion="Grammatical Range and Accuracy"
                      currentScore={6}
                    />
                  </Space>
                ),
                children: (
                  <Space orientation="vertical" className="full-width">
                    <ScorePicker value={6} compact />
                    <CriterionCommentEditor />
                  </Space>
                ),
              },
            ]}
          />
          <ErrorSummary />
          <Card className="band-summary">
            <BandBadge band={6.5} size="lg" />
            <div>
              <Text strong>Band bài tập 6.5</Text>
              <Text type="secondary" className="block-copy">
                Task 2 tính gấp đôi Task 1 · ước lượng, IELTS không công bố công
                thức chính xác
              </Text>
            </div>
          </Card>
          <Card size="small" className="incomplete-band-card">
            <Text type="secondary">Biến thể khi chưa trả đủ các phần</Text>
            <Text strong className="block-copy">
              Chưa đủ phần
            </Text>
          </Card>
          <Input.TextArea
            rows={4}
            value={generalComment}
            onChange={(event) => setGeneralComment(event.target.value)}
            placeholder="Nhận xét chung"
          />
          <CommentBankPopover onInsert={setGeneralComment} />
          <Button icon={<MessageCircle />} onClick={() => setChat(true)}>
            Trao đổi với sinh viên
          </Button>
          <div className="grading-footer">
            <Button>Lưu nháp</Button>
            <Button icon={<Pencil />} onClick={() => setRevision(true)}>
              Yêu cầu sửa
            </Button>
            <Button type="primary">Trả bài</Button>
          </div>
        </aside>
      </div>
      <Modal
        title="Yêu cầu sửa bài"
        open={revision}
        onCancel={() => setRevision(false)}
        okText="Gửi yêu cầu sửa"
      >
        <Form layout="vertical">
          <Form.Item label="Lời nhắn cho sinh viên">
            <Input.TextArea defaultValue="Phát triển thêm ví dụ ở đoạn 2 và sửa lỗi ngữ pháp đã đánh dấu." />
          </Form.Item>
          <Form.Item label="Hạn sửa">
            <DatePicker showTime />
          </Form.Item>
          <Text type="secondary">Còn 1 lần yêu cầu sửa cho bài này</Text>
        </Form>
      </Modal>
      <Modal
        title="Chấm 0"
        open={zero}
        onCancel={() => setZero(false)}
        okText="Xác nhận"
        okButtonProps={{ danger: true, disabled: !zeroReason }}
      >
        <Radio.Group
          value={zeroReason}
          onChange={(event) => setZeroReason(event.target.value)}
        >
          <Space orientation="vertical">
            <Radio value="empty">Không làm bài</Radio>
            <Radio value="language">Không viết bằng tiếng Anh</Radio>
            <Radio value="memorized">Bài học thuộc hoàn toàn</Radio>
          </Space>
        </Radio.Group>
      </Modal>
      <Drawer
        title="Gợi ý của AI (chỉ để tham khảo)"
        size={520}
        open={ai}
        onClose={() => setAi(false)}
      >
        <Alert type="info" title="Điểm cuối cùng do giáo viên quyết định" />
        {[
          "Task Response",
          "Coherence and Cohesion",
          "Lexical Resource",
          "Grammatical Range and Accuracy",
        ].map((name) => (
          <Card size="small" className="ai-card" key={name}>
            <Text strong>{name} · 6.0</Text>
            <Text type="secondary" className="block-copy">
              Nhận xét gợi ý ngắn theo bài làm.
            </Text>
          </Card>
        ))}
      </Drawer>
      <Drawer
        title="Trao đổi với sinh viên"
        size={420}
        open={chat}
        onClose={() => setChat(false)}
      >
        <ChatMessage>
          Em đã bổ sung ví dụ ở đoạn thứ hai theo nhận xét của cô.
        </ChatMessage>
        <ChatMessage mine>
          Cô đã nhận được. Em kiểm tra thêm phần kết luận nhé.
        </ChatMessage>
        <div className="compact-chat-composer">
          <Input placeholder="Nhập tin nhắn…" />
          <Button type="primary" icon={<Send />} aria-label="Gửi" />
        </div>
      </Drawer>
    </div>
  )
}

function Prompt6() {
  const [grading, setGrading] = useState(false)
  return grading ? (
    <Grading back={() => setGrading(false)} />
  ) : (
    <Queue grade={() => setGrading(true)} />
  )
}

function StudentAssignments({ write }: { write: () => void }) {
  return (
    <Shell active="Bài được giao">
      <PageHeader
        title="Bài được giao"
        description="Theo dõi bài cần làm, hạn nộp và kết quả."
      />
      <Tabs
        defaultActiveKey="todo"
        items={[
          { key: "todo", label: "Cần làm (2)" },
          { key: "soon", label: "Sắp hết hạn (1)" },
          { key: "overdue", label: "Quá hạn (0)" },
          { key: "done", label: "Đã nộp (4)" },
          { key: "revision", label: "Cần sửa (1)" },
        ]}
      />
      <div className="assignment-cards">
        {[0, 1, 2].map((index) => (
          <Card key={index}>
            <Text type="secondary">ThS. Nguyễn Thị Hạnh</Text>
            <Title level={3}>Tuần {5 - index} – Writing practice</Title>
            <div className="part-line">
              <StatusTag status="SUBMITTED" />
              <Text>Task 1 – Line graph</Text>
            </div>
            <div className="part-line">
              <StatusTag
                status={index === 1 ? "REVISION_REQUESTED" : "DRAFT"}
              />
              <Text>Task 2 – Opinion essay</Text>
            </div>
            {index === 1 ? (
              <Alert
                type="error"
                title="Bổ sung đoạn tổng quan · Hạn sửa 23:59 10/11"
              />
            ) : null}
            <Space className="card-action-row">
              <CountdownTimer tone="warning">Còn 05:12:30</CountdownTimer>
              <Button type="primary" onClick={write}>
                {index === 1 ? "Sửa bài" : "Làm bài"}
              </Button>
            </Space>
          </Card>
        ))}
      </div>
    </Shell>
  )
}

function Writing({ result }: { result: () => void }) {
  const [state, setState] = useState("normal")
  const [submit, setSubmit] = useState(false)
  const [conflict, setConflict] = useState(false)
  return (
    <div className="writing">
      <header className="writing-header">
        <Space>
          <Button type="text" icon={<ArrowLeft />} onClick={result}>
            Quay lại
          </Button>
          <div>
            <Text strong>Tuần 5 – Writing practice</Text>
            <Text type="secondary" className="block-copy">
              Task 2 – Opinion essay
            </Text>
          </div>
        </Space>
        <Space>
          <CountdownTimer tone={state === "exam" ? "error" : "neutral"}>
            {state === "exam" ? "Còn 38:12" : "Còn 05:12:30"}
          </CountdownTimer>
          <AutosaveIndicator
            state={state === "offline" ? "offline" : "saved"}
          />
          <Button
            type="primary"
            icon={<Send />}
            disabled={state === "offline" || state === "expired"}
            onClick={() => setSubmit(true)}
          >
            Nộp phần này
          </Button>
        </Space>
      </header>
      <div className="writing-options">
        <Segmented
          value={state}
          options={[
            { value: "normal", label: "Bình thường" },
            { value: "offline", label: "Ngoại tuyến" },
            { value: "submitted", label: "Đã nộp" },
            { value: "expired", label: "Quá hạn" },
            { value: "exam", label: "Chế độ thi" },
          ]}
          onChange={(value) => setState(String(value))}
        />
        <Button onClick={() => setConflict(true)}>Xung đột bản nháp</Button>
      </div>
      {state === "offline" ? (
        <Alert
          banner
          type="warning"
          icon={<WifiOff />}
          title="Mất kết nối… đã lưu trên máy"
        />
      ) : null}
      {state === "submitted" ? (
        <Alert banner type="success" title="Đã nộp lúc 23:58 07/11/2026" />
      ) : null}
      {state === "expired" ? (
        <Alert
          banner
          type="error"
          title="Đã quá hạn nộp. Liên hệ giáo viên nếu cần gia hạn."
        />
      ) : null}
      {state === "exam" ? (
        <Alert
          banner
          type="warning"
          title="Chế độ thi: rời khỏi trang sẽ được ghi lại"
        />
      ) : null}
      <Tabs
        defaultActiveKey="task2"
        items={[
          { key: "task1", label: "Task 1 – Line graph · Đã nộp ✓" },
          { key: "task2", label: "Task 2 – Opinion essay · Đang làm" },
        ]}
      />
      <div className="writing-layout">
        <aside>
          <Collapse
            className="mobile-prompt-collapse"
            items={[
              {
                key: "prompt",
                label: "Xem đề bài",
                children: (
                  <Paragraph>
                    Some people believe that university education should be free
                    for everyone. To what extent do you agree or disagree?
                  </Paragraph>
                ),
              },
            ]}
          />
          <div className="desktop-prompt-content">
            <Tag>Tối thiểu 250 từ</Tag>
            <Tag>Gợi ý 40 phút</Tag>
            <Title level={2}>Task 2 – Opinion essay</Title>
            <Paragraph>
              Some people believe that university education should be free for
              everyone. To what extent do you agree or disagree?
            </Paragraph>
            <Card className="graph-box">
              <BarChart3 />
              <Text>Ảnh biểu đồ</Text>
            </Card>
          </div>
        </aside>
        <section>
          <Input.TextArea
            className="essay-editor"
            readOnly={state === "submitted" || state === "expired"}
            defaultValue="In recent years, the question of whether higher education should be provided free of charge has become a controversial issue. While some people argue that tuition fees are necessary, I strongly believe that university education should be free for everyone..."
          />
          <div className="editor-footer">
            <WordCounter current={198} minimum={250} />
            <Text>Đã viết 24 phút</Text>
            <AutosaveIndicator
              state={state === "offline" ? "offline" : "saved"}
            />
          </div>
        </section>
      </div>
      <Modal
        title="Nộp Task 2?"
        open={submit}
        onCancel={() => setSubmit(false)}
        okText="Vẫn nộp"
        cancelText="Viết tiếp"
      >
        <Alert type="warning" title="Bài có 198 từ, ít hơn yêu cầu 250 từ." />
      </Modal>
      <Modal
        title="Bài này vừa được lưu ở một tab hoặc thiết bị khác"
        open={conflict}
        onCancel={() => setConflict(false)}
        width={760}
        footer={null}
      >
        <div className="conflict-grid">
          <Card title="Bản trên máy này (14:35)">
            <Paragraph>Bản mới trên trình duyệt…</Paragraph>
            <Button type="primary">Dùng bản trên máy này</Button>
          </Card>
          <Card title="Bản trên máy chủ (14:33)">
            <Paragraph>Bản đã đồng bộ…</Paragraph>
            <Button>Dùng bản trên máy chủ</Button>
          </Card>
        </div>
      </Modal>
    </div>
  )
}

function Result({ back }: { back: () => void }) {
  const [revision, setRevision] = useState(false)
  return (
    <div className="result-page">
      <PageHeader
        title="Kết quả · Task 2 – Opinion essay"
        description="Đã trả bài lúc 14:32 09/11/2026"
        extra={
          <Space>
            <BandBadge band={6.5} size="lg" />
            <Text strong>Khá</Text>
            <Button onClick={() => setRevision(!revision)}>Cần sửa</Button>
            <Button icon={<ArrowLeft />} onClick={back}>
              Quay lại
            </Button>
          </Space>
        }
      />
      {revision ? (
        <Alert
          type="error"
          showIcon
          title="Giáo viên yêu cầu sửa bài · Hạn sửa 23:59 10/11/2026"
          description="Lời nhắn: Bổ sung đoạn tổng quan."
          action={<Button type="primary">Sửa bài</Button>}
        />
      ) : null}
      <div className="result-layout">
        <Card>
          <Paragraph className="essay-copy">
            In recent years, higher education has become controversial.{" "}
            <span className="coded-error">
              people is<sup>VT</sup>
            </span>{" "}
            often prevented from pursuing a degree.
          </Paragraph>
          <div className="result-inline-note">
            <Tag color="red">VT</Tag>
            <Text>Sai hoà hợp chủ ngữ – động từ.</Text>
          </div>
          {!revision ? (
            <div className="inline-suggestion">
              <Text delete>people is</Text>
              <ChevronRight />
              <Text strong>people are</Text>
            </div>
          ) : null}
          {revision ? (
            <Alert
              type="info"
              title="Gợi ý sửa sẽ hiện sau khi bạn nộp bản sửa"
            />
          ) : null}
        </Card>
        <Space orientation="vertical" className="full-width">
          {[
            ["Task Response", 6.5],
            ["Coherence and Cohesion", 5],
            ["Lexical Resource", 6.5],
            ["Grammatical Range and Accuracy", 6],
          ].map(([name, band], index) => (
            <Card size="small" key={String(name)}>
              <div className="criterion-row">
                <Text strong>{name}</Text>
                <BandBadge band={Number(band)} />
              </div>
              <Text type="secondary">Nhận xét chi tiết của giáo viên.</Text>
              {index === 0 ? (
                <div className="result-checklist">
                  <div className="result-checklist-item result-achieved">
                    <span>
                      <Check />
                    </span>
                    <Text>Quan điểm rõ ràng và nhất quán</Text>
                  </div>
                  <div className="result-checklist-item">
                    <span />
                    <Text>Ý chính được phát triển với ví dụ cụ thể</Text>
                  </div>
                </div>
              ) : null}
            </Card>
          ))}
          <Alert
            type="warning"
            title="Coherence and Cohesion bị giới hạn ở 5 vì: không chia đoạn"
          />
        </Space>
      </div>
      <Card title="Lịch sử phiên bản & diff">
        <div className="filter-row">
          <Select
            defaultValue="v1-v2"
            options={[{ value: "v1-v2", label: "So sánh v1 ↔ v2" }]}
          />
          <Segmented options={["Gộp", "Song song"]} />
        </div>
        <Paragraph>
          University education should{" "}
          <Text delete className="diff-removed">
            cost less
          </Text>{" "}
          <Text className="diff-added">be free for everyone</Text>.
        </Paragraph>
      </Card>
    </div>
  )
}

function Prompt7() {
  const [view, setView] = useState<"list" | "write" | "result">("list")
  if (view === "write") return <Writing result={() => setView("result")} />
  if (view === "result") return <Result back={() => setView("list")} />
  return <StudentAssignments write={() => setView("write")} />
}

function LineChart() {
  return (
    <div className="line-chart">
      <svg viewBox="0 0 600 180" role="img" aria-label="Band theo tuần">
        <polyline
          points="20,140 100,120 180,128 260,90 340,100 420,64 500,72 580,34"
          fill="none"
          stroke="var(--brand-600)"
          strokeWidth="4"
        />
        <polyline
          points="20,154 100,146 180,118 260,124 340,92 420,100 500,58 580,68"
          fill="none"
          stroke="var(--success)"
          strokeWidth="3"
        />
      </svg>
    </div>
  )
}

function Dashboard() {
  return (
    <Shell active="Tổng quan">
      <PageHeader
        title="Tổng quan"
        description="Chào cô Hạnh, đây là tình hình lớp học hôm nay."
      />
      <div className="stats-grid four-stats">
        <StatCard label="Bài chờ chấm" value="12" delta="+4 hôm nay" />
        <StatCard label="Bài tập đang mở" value="3" delta="2 sắp hết hạn" />
        <StatCard label="Sinh viên" value="64" delta="9 trực tuyến" />
        <StatCard label="Band trung bình" value="6.1" delta="+0.2 tháng này" />
      </div>
      <div className="dashboard-two">
        <Card title="Band trung bình theo tuần">
          <LineChart />
        </Card>
        <Card title="Hoạt động gần đây">
          {students.slice(0, 5).map(([id, name], index) => (
            <div className="feed-row" key={id}>
              <Avatar size="small">{name.charAt(0)}</Avatar>
              <div>
                <Text>{name} vừa nộp Task 1</Text>
                <Text type="secondary" className="block-copy">
                  {index + 2} phút trước
                </Text>
              </div>
            </div>
          ))}
        </Card>
      </div>
    </Shell>
  )
}

function StudentAnalytics() {
  return (
    <Shell active="Kết quả học tập">
      <PageHeader
        title="Kết quả học tập"
        description="Theo dõi tiến bộ theo từng tiêu chí."
      />
      <div className="student-summary">
        <Card>
          <Space>
            <BandBadge band={6.5} size="lg" />
            <div>
              <Text>Trình độ hiện tại</Text>
              <Title level={2}>Mục tiêu 7.0</Title>
              <Progress percent={93} />
            </div>
          </Space>
        </Card>
        <StatCard label="Đã nộp" value="12 bài" delta="+3 tháng này" />
        <StatCard label="Nộp muộn" value="1" delta="8% tổng số" />
      </div>
      <div className="dashboard-two">
        <Card title="Band theo thời gian">
          <LineChart />
        </Card>
        <Card title="Trung bình theo tiêu chí">
          <Bars />
        </Card>
      </div>
    </Shell>
  )
}

function Chat() {
  return (
    <div className="chat-fullscreen">
      <aside>
        <Title level={2}>Trao đổi</Title>
        <Input prefix={<Search />} placeholder="Tìm cuộc trò chuyện" />
        <Text className="channel-label">Theo bài nộp</Text>
        {students.slice(0, 3).map(([id, name, presence], index) => (
          <Button
            type="text"
            className={index === 0 ? "channel-active" : ""}
            key={id}
          >
            <PresenceDot state={presence}>
              <Avatar>{name.charAt(0)}</Avatar>
            </PresenceDot>
            <span>
              <Text strong>Tuần 5 · Task 2 – {name}</Text>
              <Text type="secondary">Em đã sửa đoạn 2 ạ.</Text>
            </span>
            {index === 0 ? <Badge count={2} /> : null}
          </Button>
        ))}
        <Text className="channel-label">Theo lớp</Text>
        <Button type="text">
          <Avatar>LA</Avatar>
          <span>
            <Text strong>IELTS-K15A</Text>
            <Text type="secondary">Thông báo lịch học</Text>
          </span>
        </Button>
      </aside>
      <section>
        <header>
          <div>
            <Text strong>Tuần 5 · Task 2 – Nguyễn Văn An</Text>
            <Text type="secondary" className="block-copy">
              Nguyễn Văn An đang trực tuyến
            </Text>
          </div>
          <Button type="link">Mở bài nộp</Button>
        </header>
        <div className="messages">
          <Text type="secondary">Hôm nay</Text>
          <ChatMessage>Em chưa rõ cách phát triển ví dụ ạ.</ChatMessage>
          <ChatMessage mine>
            Em bắt đầu bằng một trường hợp cụ thể nhé.
          </ChatMessage>
          <ChatMessage>Em đã sửa và nộp lại rồi ạ.</ChatMessage>
          <ChatMessage mine failed>
            Cô sẽ xem lại chiều nay.
          </ChatMessage>
          <Text type="secondary">Cô Hạnh đang gõ…</Text>
        </div>
        <div className="composer">
          <Button type="text" icon={<Paperclip />} />
          <Input placeholder="Nhập tin nhắn…" />
          <Button type="primary" icon={<Send />} />
        </div>
      </section>
    </div>
  )
}

function Prompt8() {
  const [view, setView] = useState("teacher")
  return (
    <>
      <div className="subscreen-switch">
        <Segmented
          value={view}
          options={[
            { value: "teacher", label: "Dashboard giáo viên" },
            { value: "student", label: "Kết quả sinh viên" },
            { value: "chat", label: "Trao đổi" },
          ]}
          onChange={(value) => setView(String(value))}
        />
      </div>
      {view === "teacher" ? (
        <Dashboard />
      ) : view === "student" ? (
        <StudentAnalytics />
      ) : (
        <Chat />
      )}
    </>
  )
}

type UserTableProps = {
  teachers?: boolean
  add: () => void
}

function UserTable({ teachers, add }: UserTableProps) {
  return (
    <>
      <div className="filter-row">
        <Input prefix={<Search />} placeholder="Tìm người dùng" />
        <Button type="primary" icon={<Plus />} onClick={add}>
          {teachers ? "Thêm giáo viên" : "Thêm người dùng"}
        </Button>
      </div>
      <Table
        pagination={false}
        dataSource={students.map(([id, name], index) => ({
          key: id,
          id,
          name,
          index,
        }))}
        columns={[
          {
            title: "Tên đăng nhập",
            render: (_, row) => (
              <Text className="mono-copy">
                {teachers ? `gv${row.index + 1}` : row.id}
              </Text>
            ),
          },
          { title: "Họ tên", dataIndex: "name" },
          { title: "Email", render: (_, row) => `user${row.index + 1}@edu.vn` },
          {
            title: teachers ? "Số lớp" : "Vai trò",
            render: (_, row) =>
              teachers ? "2" : row.index % 2 ? "Sinh viên" : "Giáo viên",
          },
          {
            title: "Trạng thái",
            render: (_, row) =>
              row.index === 4 ? (
                <Tag color="red">Đã khoá</Tag>
              ) : (
                <Tag color="green">Hoạt động</Tag>
              ),
          },
          { title: "Đăng nhập gần nhất", render: () => "09/11/2026 · 14:32" },
          {
            title: "",
            render: () => (
              <Button
                type="text"
                icon={<MoreHorizontal />}
                aria-label="Thao tác"
              />
            ),
          },
        ]}
      />
    </>
  )
}

function Prompt9() {
  const [tab, setTab] = useState("overview")
  const [drawer, setDrawer] = useState(false)
  const [password, setPassword] = useState(false)
  return (
    <Shell active="Tổng quan" admin>
      <PageHeader
        title="Quản trị hệ thống"
        description="Quản lý người dùng, bảo mật và nhật ký."
      />
      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={[
          { key: "overview", label: "Tổng quan" },
          { key: "teachers", label: "Giáo viên" },
          { key: "users", label: "Người dùng" },
          { key: "logs", label: "Nhật ký hệ thống" },
          { key: "sessions", label: "Phiên đăng nhập" },
        ]}
      />
      {tab === "overview" ? (
        <>
          <div className="stats-grid four-stats">
            <StatCard label="Người dùng" value="196" delta="+8 tháng này" />
            <StatCard label="Lớp học" value="18" delta="16 hoạt động" />
            <StatCard label="Bài tập" value="42" delta="3 đang mở" />
            <StatCard label="Bài nộp hôm nay" value="47" delta="+12% hôm qua" />
          </div>
          <Card title="Bài nộp theo ngày">
            <div className="admin-bars">
              {Array.from({ length: 14 }, (_, i) => (
                <span key={i} data-value={30 + ((i * 13) % 65)} />
              ))}
            </div>
          </Card>
        </>
      ) : null}
      {tab === "teachers" || tab === "users" ? (
        <Card>
          <UserTable
            teachers={tab === "teachers"}
            add={() => setDrawer(true)}
          />
        </Card>
      ) : null}
      {tab === "logs" ? (
        <Card>
          <div className="filter-row">
            <Space>
              <Input placeholder="Người thực hiện" />
              <Select placeholder="Hành động" />
              <DatePicker.RangePicker />
            </Space>
            <Button icon={<Download />}>Xuất</Button>
          </div>
          <Table
            pagination={false}
            dataSource={students
              .slice(0, 4)
              .map(([id, name], index) => ({ key: id, id, name, index }))}
            expandable={{
              expandedRowRender: () => (
                <Text className="mono-copy">{`{"action":"submission.returned","band":6.5}`}</Text>
              ),
            }}
            columns={[
              { title: "Thời gian", render: () => "09/11/2026 14:32" },
              { title: "Người thực hiện", dataIndex: "name" },
              {
                title: "Hành động",
                render: (_, row) => (
                  <Tag color={row.index === 2 ? "red" : "blue"}>
                    {row.index === 2 ? "Đăng nhập thất bại" : "Trả bài"}
                  </Tag>
                ),
              },
              { title: "Đối tượng", render: () => "Task 2 · Nguyễn Văn An" },
              {
                title: "IP",
                render: (_, row) => (
                  <Text className="mono-copy">10.1.4.{21 + row.index}</Text>
                ),
              },
            ]}
          />
        </Card>
      ) : null}
      {tab === "sessions" ? (
        <Card>
          <Table
            pagination={false}
            dataSource={students
              .slice(0, 5)
              .map(([id, name], index) => ({ key: id, name, index }))}
            columns={[
              { title: "Người dùng", dataIndex: "name" },
              {
                title: "Thiết bị / trình duyệt",
                render: () => "Chrome · Windows",
              },
              {
                title: "IP",
                render: (_, row) => (
                  <Text className="mono-copy">10.1.4.{21 + row.index}</Text>
                ),
              },
              { title: "Bắt đầu", render: () => "09/11/2026 08:12" },
              { title: "Hoạt động gần nhất", render: () => "2 phút trước" },
              {
                title: "",
                render: () => <Button danger>Buộc đăng xuất</Button>,
              },
            ]}
          />
        </Card>
      ) : null}
      <Drawer
        title="Thêm giáo viên"
        size={560}
        open={drawer}
        onClose={() => setDrawer(false)}
        footer={
          <Space className="drawer-footer">
            <Button>Huỷ</Button>
            <Button type="primary" onClick={() => setPassword(true)}>
              Tạo tài khoản
            </Button>
          </Space>
        }
      >
        <Form layout="vertical">
          <Form.Item label="Tên đăng nhập">
            <Input />
          </Form.Item>
          <Form.Item label="Họ tên">
            <Input />
          </Form.Item>
          <Form.Item label="Email">
            <Input />
          </Form.Item>
        </Form>
      </Drawer>
      <Modal
        title="Tài khoản đã được tạo"
        open={password}
        onCancel={() => setPassword(false)}
        footer={<Button type="primary">Đã sao chép và lưu</Button>}
      >
        <Alert type="warning" title="Mật khẩu này chỉ hiển thị một lần" />
        <div className="temp-password">
          <Text className="mono-copy">Wr!t1ng-8X4P</Text>
          <Button icon={<Copy />} />
        </div>
      </Modal>
    </Shell>
  )
}

function ErrorCard({
  code,
  title,
  action,
}: {
  code: string
  title: string
  action: string
}) {
  return (
    <Card className="error-card">
      <span>{code}</span>
      <Title level={2}>{title}</Title>
      <Text type="secondary">Trang bạn yêu cầu không thể hiển thị.</Text>
      <Button type="primary">{action}</Button>
    </Card>
  )
}

function Prompt10() {
  const [tab, setTab] = useState("loading")
  return (
    <div className="states-page">
      <PageHeader
        title="States"
        description="Loading, lỗi, phản hồi và accessibility."
      />
      <Tabs
        activeKey={tab}
        onChange={setTab}
        items={[
          { key: "loading", label: "Đang tải" },
          { key: "errors", label: "Trang lỗi" },
          { key: "feedback", label: "Form & Toast" },
          { key: "a11y", label: "Accessibility" },
        ]}
      />
      {tab === "loading" ? (
        <div className="state-grid">
          {["Danh sách bài tập", "Ma trận tiến độ", "Màn hình chấm bài"].map(
            (name) => (
              <Card title={name} key={name}>
                <Skeleton active paragraph={{ rows: 7 }} />
              </Card>
            ),
          )}
        </div>
      ) : null}
      {tab === "errors" ? (
        <div className="error-grid">
          <ErrorCard
            code="404"
            title="Không tìm thấy trang"
            action="Về trang chủ"
          />
          <ErrorCard
            code="403"
            title="Bạn không có quyền truy cập trang này"
            action="Về trang chủ"
          />
          <ErrorCard code="500" title="Đã có lỗi xảy ra" action="Thử lại" />
        </div>
      ) : null}
      {tab === "feedback" ? (
        <div className="feedback-grid">
          <Card title="Lỗi trong form">
            <Form layout="vertical">
              <Form.Item
                label="Tên lớp"
                validateStatus="error"
                help="Vui lòng nhập tên lớp"
              >
                <Input status="error" />
              </Form.Item>
              <Button type="primary">Lưu</Button>
            </Form>
          </Card>
          <Card title="Toast">
            <Space orientation="vertical" className="full-width">
              <Alert type="success" title="Đã lưu" showIcon />
              <Alert
                type="error"
                title="Không kết nối được tới máy chủ"
                showIcon
              />
            </Space>
          </Card>
        </div>
      ) : null}
      {tab === "a11y" ? (
        <Card title="Kiểm tra khả năng truy cập">
          <Space orientation="vertical" size="large" className="full-width">
            <Alert type="success" showIcon title="Màu chữ đạt WCAG AA" />
            <div>
              <Text strong>Icon-only button có tooltip </Text>
              <Tooltip title="Chỉnh sửa">
                <Button icon={<Pencil />} aria-label="Chỉnh sửa" />
              </Tooltip>
            </div>
            <div className="focus-demo">
              <Text strong>Focus ring bàn phím</Text>
              <Input
                className="forced-focus"
                prefix={<UserCog />}
                placeholder="Tên đăng nhập / Mã sinh viên"
              />
            </div>
          </Space>
        </Card>
      ) : null}
      <Card className="consistency-card" title="Đã kiểm tra tính nhất quán">
        <Space wrap>
          <Tag color="green">Màu trạng thái</Tag>
          <Tag color="green">Màu Band</Tag>
          <Tag color="green">Lưới 4px</Tag>
          <Tag color="green">Đủ dấu tiếng Việt</Tag>
          <Tag color="green">Component dùng lại</Tag>
        </Space>
      </Card>
    </div>
  )
}

export function Prompt4To10Screens({ hidden = false }: { hidden?: boolean }) {
  const [prompt, setPrompt] = useState<PromptKey>("4")
  return (
    <main className={`all-prompts-main ${hidden ? "is-hidden" : ""}`}>
      <PromptToolbar prompt={prompt} setPrompt={setPrompt} />
      {prompt === "4" ? <Prompt4 /> : null}
      {prompt === "5" ? <Prompt5 /> : null}
      {prompt === "6" ? <Prompt6 /> : null}
      {prompt === "7" ? <Prompt7 /> : null}
      {prompt === "8" ? <Prompt8 /> : null}
      {prompt === "9" ? <Prompt9 /> : null}
      {prompt === "10" ? <Prompt10 /> : null}
    </main>
  )
}
