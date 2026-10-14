<template>
	<view class="library-page">
		<member-safe-header title="图书借阅" title-size="large" back @back="goBack" />
		<view class="search-box"><image src="/static/icon/member/library-search.svg" mode="aspectFit" /><input v-model="keyword" placeholder="搜索书号或柜号..." @input="handleSearch" /></view>
		<view class="legend-row">
			<view class="legend"><view class="legend-block borrowed" /><text>已借出</text></view>
			<view class="legend"><view class="legend-block available" /><text>可借阅</text></view>
			<view class="legend required-legend"><image src="/static/icon/member/library-info.svg" mode="aspectFit" /><text>必读</text></view>
		</view>
		<view class="tabs"><view class="tab active"><text>纸质书</text><view class="tab-line" /></view><view class="tab" @click="showEbook"><text>电子书</text></view></view>
		<view class="notice-bar"><image src="/static/icon/member/shirt-announcement.svg" mode="aspectFit" /><text>教材类书籍借阅时长是一学期，非教材类书籍借阅时间是3个月</text></view>

		<view class="book-panel">
			<view v-for="book in pageResult.items" :key="book.id" class="book-cell" :class="bookClass(book)" @click="handleBorrow(book)">{{ book.code }}</view>
			<text v-if="!pageResult.items.length" class="empty">没有找到匹配的书籍</text>
		</view>
		<view class="pagination">
			<text class="page-arrow" :class="{ disabled: pageResult.page === 1 }" @click="changePage(pageResult.page - 1)">‹</text>
			<template v-for="(item, index) in paginationItems" :key="`${item}-${index}`"><text v-if="item === 'ellipsis'" class="ellipsis">…</text><text v-else class="page-number" :class="{ active: item === pageResult.page }" @click="changePage(item)">{{ item }}</text></template>
			<text class="page-arrow" :class="{ disabled: pageResult.page === pageResult.totalPages }" @click="changePage(pageResult.page + 1)">›</text>
		</view>

		<view v-if="confirmTarget" class="modal-mask" @click="cancelBorrowConfirmation">
			<view class="confirm-modal" @click.stop>
				<text class="confirm-title">确认借阅</text>
				<text class="confirm-copy">{{ confirmationCopy }}</text>
				<view class="confirm-actions">
					<button class="cancel-button" :disabled="Boolean(pendingBookId)" @click="cancelBorrowConfirmation">取消借阅</button>
					<button class="confirm-button" :disabled="Boolean(pendingBookId)" @click="confirmBorrow">{{ pendingBookId ? '借阅中…' : '确认借阅' }}</button>
				</view>
			</view>
		</view>
		<view v-if="successResult" class="modal-mask" @click="successResult = null"><view class="success-modal" @click.stop><text class="success-copy">借阅成功！「编号{{ successResult.book.code }}」书籍是{{ successResult.categoryLabel }}书籍，请在{{ formatDate(successResult.dueOn) }}前归还。</text><button @click="successResult = null">知道了</button></view></view>
		<view v-if="ebookVisible" class="modal-mask ebook-mask" @click="closeEbook"><view class="ebook-toast" @click.stop>功能开发中，敬请期待…</view></view>
	</view>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import MemberSafeHeader from '../../../component/MemberSafeHeader.vue'
import { borrowBook, getLibraryBooks, type BorrowBookResult, type LibraryBook } from '../../../api/library'
import { buildBorrowConfirmationCopy, buildPaginationItems, filterLibraryBooks, paginate } from '../../../utils/libraryRules'

const books = ref<LibraryBook[]>([])
const keyword = ref('')
const currentPage = ref(2)
const pendingBookId = ref('')
const confirmTarget = ref<LibraryBook | null>(null)
const successResult = ref<BorrowBookResult | null>(null)
const ebookVisible = ref(false)
const filteredBooks = computed(() => filterLibraryBooks(books.value, keyword.value))
const pageResult = computed(() => paginate(filteredBooks.value, currentPage.value))
const paginationItems = computed(() => buildPaginationItems(pageResult.value.page, pageResult.value.totalPages))
const confirmationCopy = computed(() => confirmTarget.value
	? buildBorrowConfirmationCopy(confirmTarget.value.code, confirmTarget.value.category, new Date())
	: '')

const goBack = () => uni.navigateBack()
const handleSearch = () => { currentPage.value = 1 }
const changePage = (page: number) => { if (page >= 1 && page <= pageResult.value.totalPages) currentPage.value = page }
const bookClass = (book: LibraryBook) => ({ borrowed: book.status === 'borrowed', required: book.required && book.status === 'available', available: book.status === 'available' && !book.required })
const formatDate = (source: string) => { const [y, m, d] = source.split('-').map(Number); return `${y}年${m}月${d}日` }
const handleBorrow = (book: LibraryBook) => {
	if (book.status === 'borrowed') return uni.showToast({ title: '该书已借出', icon: 'none' })
	if (pendingBookId.value) return
	confirmTarget.value = book
}
const cancelBorrowConfirmation = () => {
	if (!pendingBookId.value) confirmTarget.value = null
}
const confirmBorrow = async () => {
	if (!confirmTarget.value || pendingBookId.value) return
	const book = confirmTarget.value
	pendingBookId.value = book.id
	try {
		successResult.value = await borrowBook(book.id)
		confirmTarget.value = null
		books.value = await getLibraryBooks()
	}
	catch (error: any) { uni.showToast({ title: error?.message || '借阅失败，请稍后重试', icon: 'none' }) }
	finally { pendingBookId.value = '' }
}
const showEbook = () => { ebookVisible.value = true }
const closeEbook = () => { ebookVisible.value = false }

onMounted(async () => { try { books.value = await getLibraryBooks() } catch (error: any) { uni.showToast({ title: error?.message || '图书目录加载失败', icon: 'none' }) } })
</script>

<style scoped>
.library-page { min-height: 100vh; box-sizing: border-box; padding: 0 46rpx 70rpx; background: #fff; color: #222; }
.search-box { height: 84rpx; margin-top: 50rpx; padding: 0 26rpx; border: 2rpx solid #e1e4e8; border-radius: 24rpx; background: #fafbfc; display: flex; align-items: center; }.search-box image { width: 40rpx; height: 40rpx; flex-shrink: 0; }.search-box input { flex: 1; margin-left: 22rpx; color: #333; font-size: 28rpx; }
.legend-row { margin-top: 38rpx; display: flex; justify-content: flex-end; align-items: center; gap: 44rpx; }.legend { display: flex; align-items: center; gap: 14rpx; color: #666; font-size: 27rpx; font-weight: 600; }.legend-block { width: 36rpx; height: 36rpx; border-radius: 9rpx; box-sizing: border-box; }.legend-block.borrowed { background: #ff2937; border: 2rpx solid #e90014; }.legend-block.available { background: #f0fff6; border: 2rpx solid #a9f2c7; }.required-legend { color: #fd9f24; }.required-legend image { width: 38rpx; height: 38rpx; }
.tabs { height: 102rpx; display: flex; align-items: center; justify-content: space-around; }.tab { position: relative; width: 190rpx; height: 100%; color: #888; font-size: 31rpx; font-weight: 600; display: flex; align-items: center; justify-content: center; }.tab.active { color: #333; }.tab-line { position: absolute; bottom: 7rpx; width: 64rpx; height: 6rpx; border-radius: 99rpx; background: #ff6600; }
.notice-bar { min-height: 60rpx; margin: 22rpx 10rpx 24rpx; padding: 12rpx 18rpx; box-sizing: border-box; border-radius: 8rpx; background: rgba(255,175,84,.23); color: #8a8a8a; display: flex; align-items: center; }.notice-bar image { width: 34rpx; height: 34rpx; flex-shrink: 0; }.notice-bar text { margin-left: 8rpx; font-size: 21rpx; line-height: 30rpx; white-space: nowrap; }
.book-panel { min-height: 880rpx; padding: 42rpx 34rpx; border: 2rpx solid #e7e8eb; border-radius: 48rpx; background: #f6f7f8; display: grid; grid-template-columns: repeat(5, 1fr); grid-auto-rows: 72rpx; gap: 24rpx 22rpx; box-sizing: border-box; position: relative; }.book-cell { border-radius: 14rpx; font-size: 31rpx; font-weight: 700; display: flex; align-items: center; justify-content: center; box-shadow: 0 5rpx 9rpx rgba(0,0,0,.10); box-sizing: border-box; }.book-cell.available { border: 2rpx solid #a9f2c7; background: #f0fff6; color: #08753c; }.book-cell.borrowed { border: 2rpx solid #e70014; background: #ff2937; color: #fff; }.book-cell.required { border: 3rpx solid #fda628; background: #fffaf1; color: #9f7431; }.empty { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; color: #999; font-size: 28rpx; }
.pagination { height: 92rpx; margin-top: 34rpx; display: flex; align-items: center; justify-content: center; gap: 14rpx; }.page-number { min-width: 64rpx; height: 64rpx; padding: 0 12rpx; border: 2rpx solid #e1e4e8; border-radius: 18rpx; color: #566172; font-size: 27rpx; line-height: 64rpx; text-align: center; box-sizing: border-box; }.page-number.active { border-color: #ff6600; background: #ff6600; color: #fff; box-shadow: 0 6rpx 12rpx rgba(255,102,0,.2); }.page-arrow { width: 54rpx; color: #697586; font-size: 54rpx; line-height: 64rpx; text-align: center; }.page-arrow.disabled { color: #d5d8dc; }.ellipsis { width: 42rpx; color: #8992a1; font-size: 30rpx; text-align: center; }
.modal-mask { position: fixed; inset: 0; z-index: 80; padding: 40rpx; box-sizing: border-box; background: rgba(0,0,0,.57); display: flex; align-items: center; justify-content: center; }.confirm-modal, .success-modal { width: 600rpx; box-sizing: border-box; border: 2rpx solid #ddd; border-radius: 34rpx; background: #fff; }.confirm-modal { padding: 52rpx 48rpx 46rpx; }.confirm-title { display: block; color: #111; font-size: 34rpx; line-height: 48rpx; font-weight: 700; text-align: center; }.confirm-copy, .success-copy { display: block; color: #111; font-size: 31rpx; line-height: 1.6; letter-spacing: 2rpx; }.confirm-copy { margin-top: 34rpx; }.confirm-actions { margin-top: 44rpx; display: flex; justify-content: space-between; gap: 28rpx; }.confirm-actions button, .success-modal button { height: 70rpx; margin: 0; padding: 0; border-radius: 9rpx; color: #fff; font-size: 28rpx; line-height: 70rpx; }.confirm-actions button { flex: 1; }.confirm-actions button::after, .success-modal button::after { border: 0; }.confirm-actions button[disabled] { opacity: .65; }.cancel-button { background: #969696; }.confirm-button { background: #ff6600; }.success-modal { padding: 78rpx 62rpx 54rpx; }.success-modal button { width: 170rpx; margin-top: 42rpx; background: #ff6600; }.ebook-toast { padding: 36rpx 48rpx; border-radius: 16rpx; background: #050505; color: #fdaf32; font-size: 34rpx; }.ebook-mask { z-index: 90; }
</style>
