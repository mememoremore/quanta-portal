<script>
import { guardCurrentPage, installAuthGuard } from './utils/authGuard'
import { restoreSession } from './stores/user'

export default {
  onLaunch: function () {
    restoreSession()
    installAuthGuard()
  },
  onShow: function () {
    guardCurrentPage()
  },
}
</script>

<style>
/*每个页面公共css */
</style>
