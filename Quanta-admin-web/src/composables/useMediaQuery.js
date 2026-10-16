import { onBeforeUnmount, onMounted, ref } from 'vue'

export function useMediaQuery(query) {
  const matches = ref(false)
  let media
  const update = () => { matches.value = media.matches }

  onMounted(() => {
    if (!window.matchMedia) return
    media = window.matchMedia(query)
    update()
    if (media.addEventListener) media.addEventListener('change', update)
    else media.addListener(update)
  })

  onBeforeUnmount(() => {
    if (!media) return
    if (media.removeEventListener) media.removeEventListener('change', update)
    else media.removeListener(update)
  })

  return matches
}
