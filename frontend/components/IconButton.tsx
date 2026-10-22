import { Button, Tooltip } from 'antd';

export function IconButton({ title, ...props }: { title: string } & React.ComponentProps<typeof Button>) {
  return (
    <Tooltip title={title}>
      <Button aria-label={title} {...props} />
    </Tooltip>
  );
}
