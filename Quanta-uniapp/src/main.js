import {
	createSSRApp
} from "vue";
import App from "./App.vue";
import { guardCurrentPage } from './utils/authGuard'
export function createApp() {
	const app = createSSRApp(App);
	app.mixin({
		onShow() {
			guardCurrentPage()
		},
	})
	return {
		app,
	};
}
