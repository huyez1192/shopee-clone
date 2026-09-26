export function addMessageToPage(page, message, currentUserId) {
  if (!page?.content || page.content.some((item) => item.id === message.id)) return page;
  const next = {
    ...message,
    mine: Number(message.senderId) === Number(currentUserId),
  };
  const content = [...page.content, next].sort((left, right) => {
    const time = new Date(right.createdAt).getTime() - new Date(left.createdAt).getTime();
    return time || Number(right.id) - Number(left.id);
  });
  return { ...page, content, totalElements: Number(page.totalElements || 0) + 1 };
}
