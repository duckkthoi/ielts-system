import { useState } from "react"
import {
  Alert,
  Avatar,
  Badge,
  Button,
  Card,
  Checkbox,
  Drawer,
  Dropdown,
  Empty,
  Form,
  Input,
  Modal,
  Progress,
  Segmented,
  Space,
  Table,
  Tabs,
  Tag,
  Tooltip,
  Typography,
  Upload,
} from "antd"
import type { ColumnsType } from "antd/es/table"
import {
  Bell,
  BookOpen,
  ChevronDown,
  CircleGauge,
  Download,
  FileSpreadsheet,
  GraduationCap,
  LayoutDashboard,
  Menu,
  MessageCircle,
  MoreHorizontal,
  Plus,
  Search,
  Trash2,
  UploadCloud,
  Users,
  X,
} from "lucide-react"
import { PageHeader, PresenceDot } from "./DesignSystem"

const { Text, Title, Paragraph } = Typography
const { Dragger } = Upload

type Viewport = "desktop" | "mobile"
type Screen = "list" | "detail" | "import"
type ImportStep = "upload" | "processing" | "result"

type Student = {
  key: string
  id: string
  name: string
  email: string
  joined: string
  status: "active" | "new"
  presence: "online" | "away" | "offline"
}

const students: Student[] = [
  {
    key: "1",
    id: "2151000001",
    name: "Nguyễn Văn An",
    email: "an.nv@sv.edu.vn",
    joined: "01/09/2026",
    status: "active",
    presence: "online",
  },
  {
    key: "2",
    id: "2151000002",
    name: "Trần Thị Bình",
    email: "binh.tt@sv.edu.vn",
    joined: "01/09/2026",
    status: "active",
    presence: "away",
  },
  {
    key: "3",
    id: "2151000003",
    name: "Lê Hoàng Cường",
    email: "cuong.lh@sv.edu.vn",
    joined: "02/09/2026",
    status: "new",
    presence: "offline",
  },
  {
    key: "4",
    id: "2151000004",
    name: "Phạm Thu Dung",
    email: "dung.pt@sv.edu.vn",
    joined: "02/09/2026",
    status: "active",
    presence: "online",
  },
  {
    key: "5",
    id: "2151000005",
    name: "Võ Minh Đức",
    email: "duc.vm@sv.edu.vn",
    joined: "03/09/2026",
    status: "new",
    presence: "offline",
  },
  {
    key: "6",
    id: "2151000006",
    name: "Đặng Khánh Linh",
    email: "linh.dk@sv.edu.vn",
    joined: "03/09/2026",
    status: "active",
    presence: "online",
  },
]

function TeacherSidebar({ mobile = false }: { mobile?: boolean }) {
  const items = [
    [<LayoutDashboard />, "Tổng quan"],
    [<Users />, "Lớp học"],
    [<BookOpen />, "Bài tập"],
    [<GraduationCap />, "Chấm bài"],
    [<CircleGauge />, "Thống kê"],
    [<MessageCircle />, "Trao đổi"],
  ]
  return (
    <aside className={`p3-sidebar ${mobile ? "p3-sidebar-mobile" : ""}`}>
      <div className="p3-brand">
        <span className="brand-mark">IW</span>
        <Text strong>IELTS Writing</Text>
      </div>
      <nav className="p3-menu">
        {items.map(([icon, label]) => (
          <Button
            key={String(label)}
            type="text"
            icon={icon}
            className={`p3-menu-item ${
              label === "Lớp học" ? "p3-menu-active" : ""
            }`}
          >
            {label}
          </Button>
        ))}
      </nav>
    </aside>
  )
}

function TeacherHeader({
  mobile,
  onMenu,
}: {
  mobile: boolean
  onMenu: () => void
}) {
  return (
    <header className="p3-header">
      <Space>
        {mobile ? (
          <Button
            type="text"
            icon={<Menu />}
            aria-label="Mở menu"
            onClick={onMenu}
          />
        ) : null}
        <div>
          <Text type="secondary">Giáo viên</Text>
          <Text strong className="block-copy">
            Lớp học
          </Text>
        </div>
      </Space>
      <Space>
        <span className="p3-online-dot" />
        <Text className="p3-online-copy">Trực tuyến</Text>
        <Badge count={3} size="small">
          <Tooltip title="Thông báo">
            <Button type="text" icon={<Bell />} aria-label="Thông báo" />
          </Tooltip>
        </Badge>
        <Avatar>H</Avatar>
      </Space>
    </header>
  )
}

function ClassCard({
  name,
  code,
  students: count,
  assignments,
}: {
  name: string
  code: string
  students: number
  assignments: number
}) {
  return (
    <Card className="class-card">
      <div className="class-card-top">
        <span className="class-icon">
          <Users />
        </span>
        <Dropdown
          trigger={["click"]}
          menu={{
            items: [
              { key: "edit", label: "Sửa" },
              { key: "delete", label: "Xoá", danger: true },
            ],
          }}
        >
          <Button type="text" icon={<MoreHorizontal />} aria-label="Thao tác" />
        </Dropdown>
      </div>
      <Title level={3}>{name}</Title>
      <Text className="mono-copy class-code">{code}</Text>
      <div className="class-meta">
        <Text type="secondary">
          <Users /> {count} sinh viên
        </Text>
        <Text type="secondary">
          <BookOpen /> {assignments} bài tập đang mở
        </Text>
      </div>
      <Button type="link" className="class-open-button">
        Mở lớp học <ChevronDown />
      </Button>
    </Card>
  )
}

function ClassList({
  viewport,
  onCreate,
}: {
  viewport: Viewport
  onCreate: () => void
}) {
  const [empty, setEmpty] = useState(false)
  return (
    <div className="p3-page-content">
      <PageHeader
        title="Lớp học"
        description="Quản lý thành viên, bài tập và tiến độ của các lớp bạn phụ trách."
        extra={
          <Button type="primary" icon={<Plus />} onClick={onCreate}>
            Tạo lớp
          </Button>
        }
      />
      <div className="class-toolbar">
        <Input
          prefix={<Search />}
          placeholder="Tìm theo tên hoặc mã lớp"
          allowClear
        />
        <Checkbox
          checked={empty}
          onChange={(event) => setEmpty(event.target.checked)}
        >
          Xem trạng thái trống
        </Checkbox>
      </div>
      {empty ? (
        <Card className="class-empty">
          <Empty
            description="Chưa có lớp nào"
            image={Empty.PRESENTED_IMAGE_SIMPLE}
          >
            <Button type="primary" icon={<Plus />} onClick={onCreate}>
              Tạo lớp
            </Button>
          </Empty>
        </Card>
      ) : (
        <div
          className={`class-grid ${
            viewport === "mobile" ? "class-grid-mobile" : ""
          }`}
        >
          <ClassCard
            name="IELTS K15 – Lớp A"
            code="IELTS-K15A"
            students={32}
            assignments={3}
          />
          <ClassCard
            name="IELTS K15 – Lớp B"
            code="IELTS-K15B"
            students={32}
            assignments={2}
          />
          <ClassCard
            name="IELTS K14 – Lớp nâng cao"
            code="IELTS-K14ADV"
            students={24}
            assignments={1}
          />
        </div>
      )}
    </div>
  )
}

function MobileStudentCard({ student }: { student: Student }) {
  return (
    <Card className="student-mobile-card">
      <div className="student-card-heading">
        <Space>
          <PresenceDot state={student.presence}>
            <Avatar>{student.name.split(" ").at(-1)?.charAt(0)}</Avatar>
          </PresenceDot>
          <div>
            <Text strong>{student.name}</Text>
            <Text className="mono-copy block-copy" type="secondary">
              {student.id}
            </Text>
          </div>
        </Space>
        <Button
          type="text"
          danger
          icon={<Trash2 />}
          aria-label="Xoá khỏi lớp"
        />
      </div>
      <div className="student-card-details">
        <Text type="secondary">{student.email}</Text>
        <Text type="secondary">Tham gia {student.joined}</Text>
      </div>
      {student.status === "new" ? (
        <Tag color="orange">Chưa đổi mật khẩu</Tag>
      ) : (
        <Tag color="green">Hoạt động</Tag>
      )}
    </Card>
  )
}

function ClassDetail({
  viewport,
  onImport,
}: {
  viewport: Viewport
  onImport: () => void
}) {
  const columns: ColumnsType<Student> = [
    {
      title: "",
      width: 52,
      render: (_, row) => (
        <PresenceDot state={row.presence}>
          <Avatar size="small">{row.name.split(" ").at(-1)?.charAt(0)}</Avatar>
        </PresenceDot>
      ),
    },
    {
      title: "Mã SV",
      dataIndex: "id",
      render: (value: string) => <Text className="mono-copy">{value}</Text>,
    },
    { title: "Họ tên", dataIndex: "name" },
    { title: "Email", dataIndex: "email" },
    { title: "Ngày tham gia", dataIndex: "joined" },
    {
      title: "Trạng thái tài khoản",
      dataIndex: "status",
      render: (status: Student["status"]) =>
        status === "new" ? (
          <Tag color="orange">Chưa đổi mật khẩu</Tag>
        ) : (
          <Tag color="green">Hoạt động</Tag>
        ),
    },
    {
      title: "",
      width: 56,
      render: () => (
        <Tooltip title="Xoá khỏi lớp">
          <Button
            type="text"
            danger
            icon={<Trash2 />}
            aria-label="Xoá khỏi lớp"
          />
        </Tooltip>
      ),
    },
  ]
  return (
    <div className="p3-page-content">
      <PageHeader
        title="IELTS K15 – Lớp A"
        description="32 sinh viên · Mã lớp IELTS-K15A"
        extra={
          <Space wrap>
            <Button type="text" icon={<Download />}>
              Tải file mẫu
            </Button>
            <Button icon={<Plus />}>Thêm sinh viên</Button>
            <Button type="primary" icon={<UploadCloud />} onClick={onImport}>
              Import sinh viên
            </Button>
          </Space>
        }
      />
      <Tabs
        defaultActiveKey="members"
        items={[
          {
            key: "members",
            label: "Thành viên (32)",
            children:
              viewport === "mobile" ? (
                <div className="student-mobile-list">
                  {students.map((student) => (
                    <MobileStudentCard student={student} key={student.id} />
                  ))}
                </div>
              ) : (
                <Card className="members-table-card">
                  <div className="members-toolbar">
                    <Input
                      prefix={<Search />}
                      placeholder="Tìm sinh viên"
                      allowClear
                    />
                    <Text type="secondary">Hiển thị 6 trên 32 sinh viên</Text>
                  </div>
                  <Table
                    columns={columns}
                    dataSource={students}
                    pagination={false}
                    scroll={{ x: 900 }}
                  />
                </Card>
              ),
          },
          {
            key: "assignments",
            label: "Bài tập (5)",
            children: <Empty description="Chưa có dữ liệu bài tập mẫu" />,
          },
          {
            key: "statistics",
            label: "Thống kê",
            children: <Empty description="Chưa có dữ liệu thống kê mẫu" />,
          },
        ]}
      />
    </div>
  )
}

function ResultStat({
  label,
  value,
  tone,
}: {
  label: string
  value: number
  tone: string
}) {
  return (
    <Card className={`import-stat import-stat-${tone}`}>
      <Text type="secondary">{label}</Text>
      <Title level={2}>{value}</Title>
    </Card>
  )
}

function ImportContent({
  step,
  setStep,
  onClose,
}: {
  step: ImportStep
  setStep: (step: ImportStep) => void
  onClose: () => void
}) {
  if (step === "upload") {
    return (
      <div className="import-content">
        <Dragger
          accept=".xlsx,.csv"
          maxCount={1}
          beforeUpload={() => false}
          showUploadList={false}
        >
          <UploadCloud className="upload-icon" />
          <Paragraph>
            Kéo thả file .xlsx hoặc .csv vào đây, hoặc bấm để chọn file
          </Paragraph>
          <Text type="secondary">
            Cột bắt buộc: ma_sv, ho_ten. Cột tuỳ chọn: email. Tối đa 10 MB.
          </Text>
        </Dragger>
        <div className="import-options">
          <Button type="link" icon={<Download />}>
            Tải file mẫu
          </Button>
          <Checkbox defaultChecked>
            Gửi thông tin đăng nhập qua email cho sinh viên mới
          </Checkbox>
        </div>
        <div className="import-actions">
          <Button onClick={onClose}>Huỷ</Button>
          <Button type="primary" onClick={() => setStep("processing")}>
            Import
          </Button>
        </div>
      </div>
    )
  }
  if (step === "processing") {
    return (
      <div className="import-content import-processing">
        <span className="processing-file-icon">
          <FileSpreadsheet />
        </span>
        <Title level={3}>Đang import danh sách sinh viên</Title>
        <Text type="secondary">danh-sach-k15a.xlsx · 45 dòng</Text>
        <Progress percent={62} status="active" />
        <Text>Đang xử lý 28/45 dòng…</Text>
        <Button onClick={() => setStep("result")}>Xem kết quả mô phỏng</Button>
      </div>
    )
  }
  return (
    <div className="import-content">
      <Alert
        type="success"
        showIcon
        title="Import hoàn tất"
        description="44/45 dòng đã được xử lý thành công."
      />
      <div className="import-stat-grid">
        <ResultStat label="Tạo mới" value={40} tone="success" />
        <ResultStat label="Đã có tài khoản" value={3} tone="info" />
        <ResultStat label="Đã trong lớp" value={1} tone="neutral" />
        <ResultStat label="Lỗi" value={1} tone="error" />
      </div>
      <Table
        size="small"
        pagination={false}
        dataSource={[
          {
            key: "17",
            row: 17,
            id: "21A",
            reason: "Mã sinh viên không hợp lệ",
          },
        ]}
        columns={[
          { title: "Dòng", dataIndex: "row" },
          { title: "Mã SV", dataIndex: "id" },
          { title: "Lý do", dataIndex: "reason" },
        ]}
      />
      <Alert
        type="warning"
        showIcon
        title="File tài khoản chứa mật khẩu tạm và chỉ tải được một lần. Hãy tải về và lưu cẩn thận."
        action={
          <Button type="primary" icon={<Download />}>
            Tải file tài khoản (.xlsx)
          </Button>
        }
      />
      <Text type="secondary">
        Email đã được đưa vào hàng gửi cho 40 sinh viên.
      </Text>
      <div className="import-actions">
        <Button onClick={onClose}>Đóng</Button>
      </div>
    </div>
  )
}

function ImportShowcase({ onOpen }: { onOpen: () => void }) {
  return (
    <div className="p3-page-content">
      <PageHeader
        title="Import sinh viên"
        description="Xem trước đầy đủ ba trạng thái của quy trình import."
        extra={
          <Button type="primary" icon={<UploadCloud />} onClick={onOpen}>
            Mở quy trình import
          </Button>
        }
      />
      <div className="import-explainer-grid">
        {[
          ["1", "Chọn file", "Tải lên file .xlsx hoặc .csv theo mẫu."],
          ["2", "Đang xử lý", "Theo dõi số dòng đã được kiểm tra."],
          ["3", "Kết quả", "Tải file tài khoản và xem lỗi chi tiết."],
        ].map(([number, title, copy]) => (
          <Card key={number}>
            <span className="import-step-number">{number}</span>
            <Title level={3}>{title}</Title>
            <Text type="secondary">{copy}</Text>
          </Card>
        ))}
      </div>
    </div>
  )
}

export function Prompt3Screens({ hidden = false }: { hidden?: boolean }) {
  const [screen, setScreen] = useState<Screen>("list")
  const [viewport, setViewport] = useState<Viewport>("desktop")
  const [createOpen, setCreateOpen] = useState(false)
  const [importOpen, setImportOpen] = useState(false)
  const [importStep, setImportStep] = useState<ImportStep>("upload")
  const [confirmClose, setConfirmClose] = useState(false)
  const [mobileMenu, setMobileMenu] = useState(false)

  const closeImport = () => {
    if (importStep === "result") {
      setConfirmClose(true)
    } else {
      setImportOpen(false)
      setImportStep("upload")
    }
  }

  return (
    <main className={`prompt3-main ${hidden ? "is-hidden" : ""}`}>
      <Card className="p3-toolbar">
        <div>
          <Text className="eyebrow">PROMPT 3</Text>
          <Title level={2}>Giáo viên · Lớp học & import sinh viên</Title>
        </div>
        <Segmented
          value={viewport}
          options={[
            { value: "desktop", label: "Desktop" },
            { value: "mobile", label: "Mobile" },
          ]}
          onChange={(value) => setViewport(value as Viewport)}
        />
        <Tabs
          activeKey={screen}
          onChange={(key) => setScreen(key as Screen)}
          items={[
            { key: "list", label: "Danh sách lớp" },
            { key: "detail", label: "Chi tiết lớp" },
            { key: "import", label: "Luồng import" },
          ]}
        />
      </Card>
      <div className={`p3-frame p3-frame-${viewport}`}>
        {viewport === "desktop" ? <TeacherSidebar /> : null}
        <div className="p3-main-column">
          <TeacherHeader
            mobile={viewport === "mobile"}
            onMenu={() => setMobileMenu(true)}
          />
          {screen === "list" ? (
            <ClassList
              viewport={viewport}
              onCreate={() => setCreateOpen(true)}
            />
          ) : null}
          {screen === "detail" ? (
            <ClassDetail
              viewport={viewport}
              onImport={() => {
                setImportStep("upload")
                setImportOpen(true)
              }}
            />
          ) : null}
          {screen === "import" ? (
            <ImportShowcase
              onOpen={() => {
                setImportStep("upload")
                setImportOpen(true)
              }}
            />
          ) : null}
        </div>
      </div>

      <Drawer
        title="Tạo lớp"
        size={600}
        open={createOpen}
        onClose={() => setCreateOpen(false)}
        footer={
          <Space className="drawer-footer">
            <Button onClick={() => setCreateOpen(false)}>Huỷ</Button>
            <Button type="primary">Tạo lớp</Button>
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
        title="Import sinh viên"
        width={760}
        open={importOpen}
        onCancel={closeImport}
        footer={null}
        destroyOnHidden
      >
        <div className="import-state-switcher">
          <Text type="secondary">Xem trạng thái:</Text>
          <Segmented
            size="small"
            value={importStep}
            options={[
              { value: "upload", label: "Chọn file" },
              { value: "processing", label: "Đang xử lý" },
              { value: "result", label: "Kết quả" },
            ]}
            onChange={(value) => setImportStep(value as ImportStep)}
          />
        </div>
        <ImportContent
          step={importStep}
          setStep={setImportStep}
          onClose={closeImport}
        />
      </Modal>

      <Modal
        title="Bạn chưa tải file tài khoản"
        open={confirmClose}
        onCancel={() => setConfirmClose(false)}
        footer={[
          <Button key="stay" onClick={() => setConfirmClose(false)}>
            Ở lại
          </Button>,
          <Button
            key="close"
            danger
            type="primary"
            onClick={() => {
              setConfirmClose(false)
              setImportOpen(false)
              setImportStep("upload")
            }}
          >
            Vẫn đóng
          </Button>,
        ]}
      >
        <Paragraph>
          Nếu đóng, sẽ phải đặt lại mật khẩu cho từng sinh viên.
        </Paragraph>
      </Modal>

      <Drawer
        rootClassName="mobile-nav-drawer"
        placement="left"
        size={280}
        open={mobileMenu}
        closeIcon={<X />}
        onClose={() => setMobileMenu(false)}
      >
        <TeacherSidebar mobile />
      </Drawer>
    </main>
  )
}
