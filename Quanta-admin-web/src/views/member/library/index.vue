<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { borrowBook, getBooks } from '@/api/portal/library'

const books = ref([])
const total = ref(0)
const loading = ref(true)
const errorMessage = ref('')
const selectedBook = ref(null)
const dueTime = ref('')
const borrowingId = ref(null)
const query = reactive({ bookName: '', bookType: '', availability: '', pageNum: 1, pageSize: 12 })

async function loadBooks() {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.bookName.trim()) params.bookName = query.bookName.trim()
    if (query.bookType) params.bookType = query.bookType
    if (query.availability === 'available') params.status = '0'
    const result = await getBooks(params)
    books.value = query.availability === 'available'
      ? result.rows.filter((book) => book.available)
      : query.availability === 'unavailable'
        ? result.rows.filter((book) => !book.available)
        : result.rows
    total.value = result.total
  } catch (error) {
    errorMessage.value = error.message || '图书加载失败'
  } finally {
    loading.value = false
  }
}

function search() {
  query.pageNum = 1
  loadBooks()
}

function chooseBook(book) {
  selectedBook.value = book
  dueTime.value = ''
}

async function confirmBorrow() {
  if (!selectedBook.value || !dueTime.value || borrowingId.value) return
  borrowingId.value = selectedBook.value.bookId
  try {
    await borrowBook(selectedBook.value.bookId, dueTime.value)
    ElMessage.success('图书借阅成功')
    selectedBook.value = null
    dueTime.value = ''
    await loadBooks()
  } catch (error) {
    ElMessage.error(error.message || '图书借阅失败')
  } finally {
    borrowingId.value = null
  }
}

onMounted(loadBooks)
</script>

<template>
  <section class="member-service-page">
    <header class="member-service-heading">
      <div><p>QUANTA LIBRARY</p><h1>图书借阅</h1><span>查询馆藏并确认应还日期后提交借阅。</span></div>
    </header>
    <form class="member-filter-bar portal-card" @submit.prevent="search">
      <label>关键词<input v-model="query.bookName" type="search" placeholder="书名 / ISBN" /></label>
      <label>类别<select v-model="query.bookType"><option value="">全部类别</option><option value="TEXTBOOK">教材类</option><option value="GENERAL">非教材类</option></select></label>
      <label>状态<select v-model="query.availability"><option value="">全部状态</option><option value="available">可借</option><option value="unavailable">暂不可借</option></select></label>
      <button class="member-primary-button" type="submit">查询</button>
    </form>
    <p v-if="errorMessage" class="member-feedback is-error" role="alert">{{ errorMessage }} <button type="button" @click="loadBooks">重试</button></p>

    <div v-if="loading" class="member-service-state portal-card">正在查询图书…</div>
    <div v-else-if="books.length" class="member-book-grid">
      <article v-for="book in books" :key="book.bookId" class="member-book-card portal-card">
        <span class="member-book-card__type">{{ book.bookType === 'TEXTBOOK' ? '教材类' : '非教材类' }}</span>
        <h2>{{ book.bookName || `图书 #${book.bookId}` }}</h2>
        <p>{{ book.author || '作者待补充' }} · {{ book.publisher || '出版社待补充' }}</p>
        <dl><div><dt>位置</dt><dd>{{ book.locationDesc || '待确认' }}</dd></div><div><dt>可借</dt><dd>{{ book.availableCount }} / {{ book.totalCount }}</dd></div></dl>
        <button :data-testid="`borrow-book-${book.bookId}`" class="member-primary-button" type="button" :disabled="!book.available" @click="chooseBook(book)">
          {{ book.available ? '选择并借阅' : '暂不可借' }}
        </button>
      </article>
    </div>
    <div v-else class="member-service-state portal-card"><strong>没有找到符合条件的图书</strong><p>可以尝试清空筛选条件后重新查询。</p></div>

    <section v-if="selectedBook" class="member-confirm-panel portal-card" aria-labelledby="borrow-confirm-title">
      <div><p>借阅确认</p><h2 id="borrow-confirm-title">{{ selectedBook.bookName }}</h2><span>提交前请确认应还日期。</span></div>
      <label>应还日期<input v-model="dueTime" name="dueTime" type="date" required /></label>
      <div class="member-confirm-panel__actions"><button type="button" class="member-secondary-button" @click="selectedBook = null">取消</button><button data-testid="confirm-borrow" type="button" class="member-primary-button" :disabled="!dueTime || Boolean(borrowingId)" @click="confirmBorrow">{{ borrowingId ? '提交中…' : '确认借阅' }}</button></div>
    </section>
  </section>
</template>

<style src="../services/services.css"></style>
