const proofSvg = `<svg xmlns="http://www.w3.org/2000/svg" width="520" height="320"><rect width="100%" height="100%" fill="#f7f8fb"/><rect x="55" y="35" width="410" height="250" rx="16" fill="white" stroke="#e2e6ef"/><text x="260" y="105" text-anchor="middle" font-size="25" fill="#1b2234">支付凭证</text><text x="260" y="155" text-anchor="middle" font-size="18" fill="#697386">Quanta 塔服 ¥45.00</text><text x="260" y="205" text-anchor="middle" font-size="16" fill="#05a955">支付成功</text></svg>`
export const mockProofUrl = `data:image/svg+xml;charset=utf-8,${encodeURIComponent(proofSvg)}`

const initialOrders = [
  { orderId: 1, nickName: '陈思远', selectedColor: '藏青色', selectedSize: 'L', unitPrice: 45, orderTime: '2025-04-01 10:00', remark: '-', status: 'APPROVED', paymentProofUrl: mockProofUrl },
  { orderId: 2, nickName: '刘雨欣', selectedColor: '白色', selectedSize: 'S', unitPrice: 45, orderTime: '2025-04-01 10:30', remark: '已上传微信截图', status: 'APPROVED', paymentProofUrl: mockProofUrl },
  { orderId: 3, nickName: '张伟', selectedColor: '藏青色', selectedSize: 'XL', unitPrice: 45, orderTime: '2025-04-01 11:00', remark: '-', status: 'APPROVED', paymentProofUrl: mockProofUrl },
  { orderId: 4, nickName: '王芳', selectedColor: '白色', selectedSize: 'M', unitPrice: 45, orderTime: '2025-04-01 14:00', remark: '-', status: 'DRAFT', paymentProofUrl: '' },
  { orderId: 5, nickName: '赵明', selectedColor: '藏青色', selectedSize: 'M', unitPrice: 45, orderTime: '2025-04-02 09:00', remark: '-', status: 'DRAFT', paymentProofUrl: '' },
  { orderId: 6, nickName: '孙丽', selectedColor: '白色', selectedSize: 'XS', unitPrice: 45, orderTime: '2025-04-02 10:00', remark: '已上传支付宝截图', status: 'SUBMITTED', paymentProofUrl: mockProofUrl },
]

export let mockClothingOrders = structuredClone(initialOrders)
export function resetMockClothingOrders() { mockClothingOrders = structuredClone(initialOrders) }
