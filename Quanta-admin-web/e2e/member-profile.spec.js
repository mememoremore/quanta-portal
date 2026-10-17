import { test, expect, loginAs } from './support/fixture'
import { readFileSync } from 'node:fs'

test('cloud profile saves across reloads and does not accept identity fields', async ({page,api},testInfo)=>{
  await loginAs(page,'member')
  await page.goto('/member/profile')
  await expect(page.getByRole('heading',{name:'个人中心'})).toBeVisible()
  await page.getByLabel('一句话介绍',{exact:true}).fill('和塔友一起做有意思的项目')
  await page.screenshot({path:testInfo.outputPath('profile-desktop.png'),fullPage:true})
  await page.getByLabel('联系邮箱（选填）',{exact:true}).fill('member@example.com')
  await page.getByRole('button',{name:'保存修改',exact:true}).click()
  await expect(page.getByText('个人档案已保存',{exact:true})).toBeVisible()
  expect(api.profiles[102].userId).toBeUndefined()
  expect(api.profiles[102].showContactEmail).toBe(false)
  await page.reload()
  await expect(page.getByLabel('一句话介绍',{exact:true})).toHaveValue('和塔友一起做有意思的项目')
  await page.getByRole('link',{name:'查看我的成员主页'}).click()
  await expect(page.locator('h1')).toHaveText('塔员同学')
  await expect(page.locator('.member-public-card')).not.toContainText('member@example.com')
  await expect(page.getByRole('button',{name:'保存修改',exact:true})).toHaveCount(0)
  await page.screenshot({path:testInfo.outputPath('member-desktop.png'),fullPage:true})
  for(const width of [390,320]) {
    await page.setViewportSize({width,height:844})
    expect(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth)).toBe(false)
    if(width===390)await page.screenshot({path:testInfo.outputPath('member-mobile.png'),fullPage:true})
  }
  await page.getByRole('button',{name:'返回个人中心'}).click()
  expect(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth)).toBe(false)
  await page.setViewportSize({width:390,height:844})
  await page.screenshot({path:testInfo.outputPath('profile-mobile.png'),fullPage:true})
})

test('avatar requires confirmation, retains old image after failure and synchronizes on success',async({page,api})=>{
  await loginAs(page,'member')
  await page.goto('/member/profile')
  await page.getByRole('button',{name:'更换头像',exact:true}).first().click()
  await page.getByLabel('选择头像图片').setInputFiles({name:'avatar.jpg',mimeType:'image/jpeg',buffer:readFileSync(new URL('../public/quanta-logo-small.jpg',import.meta.url))})
  await expect(page.getByRole('button',{name:'确认上传',exact:true})).toBeEnabled()
  expect(api.requests).not.toContain('POST /system/user/profile/avatar')
  api.avatarFailure=true
  await page.getByRole('button',{name:'确认上传',exact:true}).click()
  await expect(page.getByRole('alert').filter({hasText:'模拟头像上传失败'})).toBeVisible()
  expect(api.avatars[102]).toBeUndefined()
  api.avatarFailure=false
  await page.getByRole('button',{name:'确认上传',exact:true}).click()
  await expect(page.getByRole('dialog')).toBeHidden()
  await expect(page.locator('.portal-user-menu__trigger .member-avatar img')).toBeVisible()
  await page.goto('/member/community')
  await expect(page.locator('.community-post-card .member-avatar img')).toBeVisible()
  await page.getByRole('link',{name:'一起优化移动端报名页',exact:true}).click()
  await expect(page.locator('.community-detail__author .member-avatar img')).toBeVisible()
})

test('author and comment links visit other members and preserve article return path',async({page,api})=>{
  api.forumPost.authorId=3;api.forumPost.authorName='测试成员3'
  api.forumComments=[{commentId:1,authorId:4,authorName:'测试成员4',content:'我来参与讨论',parentId:null}]
  await loginAs(page,'member')
  await page.goto('/member/community')
  await page.getByRole('link',{name:'一起优化移动端报名页',exact:true}).click()
  await page.locator('.community-detail__author').getByRole('link').click()
  await expect(page).toHaveURL(/\/member\/people\/3$/)
  await expect(page.getByRole('button',{name:'返回刚才的文章'})).toBeVisible()
  await page.getByRole('button',{name:'返回刚才的文章'}).click()
  await expect(page).toHaveURL(/\/member\/community\/1$/)
  await page.locator('.community-comment').getByRole('link',{name:'查看测试成员4的成员主页'}).first().click()
  await expect(page.locator('h1')).toHaveText('测试成员4')
})

test('directory → profile → posts → article → profile works on mobile without overflow',async({page,api})=>{
  await page.setViewportSize({width:390,height:844})
  api.forumPost.authorId=3;api.forumPost.authorName='测试成员3'
  await loginAs(page,'member')
  await page.goto('/member/directory')
  await page.getByRole('searchbox').fill('测试成员3')
  await page.locator('.member-directory__card').click()
  await expect(page.getByRole('button',{name:'返回通讯录'})).toBeVisible()
  await page.getByRole('tab',{name:'发布的帖子'}).click()
  await expect.poll(()=>page.evaluate(()=>history.state.memberReturn)).toContain('/member/directory')
  await page.getByRole('link',{name:'一起优化移动端报名页',exact:true}).click()
  await page.getByRole('link',{name:'← 返回成员主页'}).click()
  await expect(page.getByRole('tab',{name:'发布的帖子'})).toHaveAttribute('aria-selected','true')
  expect(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth)).toBe(false)
  await page.getByRole('button',{name:'返回通讯录'}).click()
  await expect(page.getByRole('searchbox')).toHaveValue('测试成员3')
})

test('cloud failures are visible and do not pretend to save locally',async({page,api})=>{
  api.profileUnavailable=true
  await loginAs(page,'member')
  await page.goto('/member/profile')
  await expect(page.getByRole('alert').filter({hasText:'云端个人档案接口尚未接通'})).toBeVisible()
  await expect(page.getByRole('button',{name:'保存修改',exact:true})).toBeDisabled()
})

test('anonymous and freshman accounts cannot open member profiles',async({page,api})=>{
  await page.goto('/member/people/3')
  await expect(page).toHaveURL(/\/login\/member\?redirect=/)
  await loginAs(page,'freshman')
  await page.goto('/member/people/3')
  await expect(page).toHaveURL(/\/freshman\/home$/)
  expect(api.requests).not.toContain('GET /qt/member/3/profile')
})
