<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, useId } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  options: {
    type: Array,
    default: () => [],
  },
  name: {
    type: String,
    required: true,
  },
  invalid: Boolean,
})

const emit = defineEmits(['update:modelValue'])
const rootRef = ref(null)
const triggerRef = ref(null)
const open = ref(false)
const activeIndex = ref(-1)
const listboxId = `department-select-${useId()}`

const selectedIndex = computed(() =>
  props.options.findIndex((option) => option.value === props.modelValue),
)
const selectedLabel = computed(() =>
  props.options.find((option) => option.value === props.modelValue)?.label || '请选择',
)
const activeOptionId = computed(() =>
  open.value && activeIndex.value >= 0 ? `${listboxId}-option-${activeIndex.value}` : undefined,
)

function showMenu() {
  open.value = true
  activeIndex.value = selectedIndex.value >= 0 ? selectedIndex.value : 0
}

function closeMenu() {
  open.value = false
  activeIndex.value = -1
}

function toggleMenu() {
  if (open.value) closeMenu()
  else showMenu()
}

function selectOption(option) {
  emit('update:modelValue', option.value)
  closeMenu()
  nextTick(() => triggerRef.value?.focus())
}

function moveActive(step) {
  if (!props.options.length) return
  if (!open.value) {
    showMenu()
    return
  }
  activeIndex.value = (activeIndex.value + step + props.options.length) % props.options.length
}

function handleKeydown(event) {
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    moveActive(1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    moveActive(-1)
  } else if (event.key === 'Enter' || event.key === ' ') {
    event.preventDefault()
    if (!open.value) showMenu()
    else if (activeIndex.value >= 0) selectOption(props.options[activeIndex.value])
  } else if (event.key === 'Escape' && open.value) {
    event.preventDefault()
    closeMenu()
  }
}

function handleOutsidePointer(event) {
  if (open.value && !rootRef.value?.contains(event.target)) closeMenu()
}

function focus() {
  triggerRef.value?.focus()
}

defineExpose({ focus })

onMounted(() => document.addEventListener('pointerdown', handleOutsidePointer))
onBeforeUnmount(() => document.removeEventListener('pointerdown', handleOutsidePointer))
</script>

<template>
  <div ref="rootRef" class="department-select" :class="{ 'is-open': open, 'is-invalid': invalid }">
    <button
      ref="triggerRef"
      type="button"
      class="department-select__trigger"
      data-testid="department-select-trigger"
      :name="name"
      role="combobox"
      aria-haspopup="listbox"
      :aria-expanded="String(open)"
      :aria-controls="listboxId"
      :aria-activedescendant="activeOptionId"
      @click="toggleMenu"
      @keydown="handleKeydown"
    >
      <span :class="{ 'is-placeholder': !modelValue }">{{ selectedLabel }}</span>
      <svg viewBox="0 0 20 20" aria-hidden="true">
        <path d="m5.5 7.5 4.5 4.5 4.5-4.5" />
      </svg>
    </button>

    <Transition name="department-select-menu">
      <div v-if="open" :id="listboxId" class="department-select__menu" role="listbox">
        <button
          v-for="(option, index) in options"
          :id="`${listboxId}-option-${index}`"
          :key="option.value"
          type="button"
          class="department-select__option"
          :class="{
            'is-active': index === activeIndex,
            'is-selected': option.value === modelValue,
          }"
          role="option"
          :aria-selected="String(option.value === modelValue)"
          :data-value="option.value"
          @mouseenter="activeIndex = index"
          @click="selectOption(option)"
        >
          <span>{{ option.label }}</span>
          <svg v-if="option.value === modelValue" viewBox="0 0 20 20" aria-hidden="true">
            <path d="m4.5 10.2 3.4 3.4 7.6-7.5" />
          </svg>
        </button>
      </div>
    </Transition>
  </div>
</template>
