package c2cw.cdydownload

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch

@Composable internal fun SettingsPage(vm: MainViewModel, topInset: Dp, onClipboard: (Boolean) -> Unit) {
    val context = LocalContext.current
    val parallelism by vm.store.parallelism.collectAsStateWithLifecycle()
    var selectParallelism by remember { mutableStateOf(false) }
    if(selectParallelism) AlertDialog(onDismissRequest = { selectParallelism = false }, shape = RoundedCornerShape(20.dp),
        title = { Text("最多同时下载") },
        text = { Column {
            (1..6).forEach { count ->
                Row(Modifier.fillMaxWidth().selectable(selected = parallelism == count, role = Role.RadioButton,
                    onClick = { vm.setParallelism(count); selectParallelism = false }).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = parallelism == count, onClick = null)
                    Text("$count 个任务" + if(count == 3) "（默认）" else "", Modifier.padding(start = 12.dp))
                }
            }
        } }, confirmButton = {}, dismissButton = { TextButton(onClick = { selectParallelism = false }) { Text("取消") } })
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = topInset)
        .padding(horizontal = 16.dp).padding(bottom = BottomBarSpace + 12.dp).testTag("settingsPage"),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("下载")
        UiGroup {
            SettingRow("最多同时下载", value = "$parallelism 个任务",
                description = "调低后，进行中的任务继续完成，后续按新上限开始。",
                onClick = { selectParallelism = true }, modifier = Modifier.testTag("parallelismSetting"))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingRow("将下载时间设为图片拍摄时间",
                description = "适用于之后下载的图片和实况图，覆盖原有拍摄时间，方便在相册中查找。不支持写入的格式仅设置相册时间。") {
                MiuixSwitch(checked = vm.imageDownloadTime, onCheckedChange = vm::changeImageDownloadTime)
            }
        }
        AccountSettings()
        SectionLabel("链接与提醒")
        UiGroup {
            SettingRow("自动检测剪贴板", description = "打开应用或回到前台时，识别作品链接并提示下载。仅识别 B 站、抖音、小红书、YouTube、微博和微信视频号，普通文本与敏感内容不会保存。") {
                MiuixSwitch(checked = vm.clipboardEnabled, onCheckedChange = onClipboard)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingRow("在其他应用中添加下载", description = "系统不允许后台读取剪贴板。在视频应用中选择「分享 → 更多 → CDy下载器」，确认后即可下载。")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SettingRow("下载通知", description = "管理进度与完成提醒", onClick = {
                context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            })
        }
        SectionLabel("外观")
        AccentColorCard(vm)
        SectionLabel("保存位置")
        UiGroup {
            SaveFolderChoice(vm, SaveKind.VIDEO, "视频", "saveFolderVideo")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SaveFolderChoice(vm, SaveKind.IMAGE, "图片", "saveFolderImage")
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SaveFolderChoice(vm, SaveKind.MUSIC, "音乐", "saveFolderMusic")
        }
        Text("视频、图片、音乐分开选择。选系统目录不会再建文件夹；「选择文件夹」可以存到手机里任意位置。",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp))
        SectionLabel("文件")
        FileNameTemplateCard(vm)
        c2cw.cdydownload.update.AboutUpdateCard()
    }
}

@Composable private fun AccentColorCard(vm: MainViewModel) {
    UiGroup {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("主题色", style = MaterialTheme.typography.titleMedium)
            val modes = AccentMode.entries
            SegmentedControl(modes.map { it.label }, modes.indexOf(vm.accentMode), Modifier.fillMaxWidth(),
                tagPrefix = "accentMode") { vm.changeAccentMode(modes[it]) }
            Text(when(vm.accentMode) {
                AccentMode.WALLPAPER -> "使用系统根据壁纸生成的颜色；系统不提供时用蓝色。"
                AccentMode.RANDOM -> "每次打开应用随机换一种颜色，不会和上次相同。"
                AccentMode.CUSTOM -> "点色块或拖动下面的色条挑一个颜色，马上生效。"
            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedVisibility(vm.accentMode == AccentMode.CUSTOM, enter = UiMotion.expand, exit = UiMotion.collapse) {
                CustomColorPicker(vm.customAccent, vm::changeCustomAccent)
            }
        }
    }
}

@Composable private fun SaveFolderChoice(vm: MainViewModel, kind: SaveKind, label: String, tag: String) {
    var picking by remember { mutableStateOf(false) }
    val tree = vm.saveTree(kind)
    val folder = vm.saveFolder(kind)
    val lost = remember(tree) { vm.saveTreeLost(kind) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if(uri != null) vm.chooseSaveTree(kind, uri)
    }
    SettingRow("${label}文件夹", value = saveLocationLabel(kind, folder, tree, SaveTree::label),
        description = when {
            lost -> "文件夹权限已失效，下载会失败，请重新选择"
            tree.isNotBlank() -> "保存在所选文件夹"
            folder.isBlank() -> "直接保存在系统目录"
            folder == DEFAULT_SAVE_FOLDER -> "保存在系统目录下的「$DEFAULT_SAVE_FOLDER」"
            else -> "沿用之前填写的文件夹名称"
        }, onClick = { picking = true }, modifier = Modifier.testTag(tag))
    if(picking) SaveFolderDialog(label, kind, folder, tree, onDismiss = { picking = false },
        onPreset = { vm.changeSaveFolder(kind, it); picking = false },
        onPick = { picking = false; runCatching { launcher.launch(null) }.onFailure { vm.notice = "无法打开系统文件夹选择器" } })
}

@Composable private fun SaveFolderDialog(label: String, kind: SaveKind, folder: String, tree: String,
    onDismiss: () -> Unit, onPreset: (String) -> Unit, onPick: () -> Unit) {
    val root = saveRoot(kind)
    val legacy = tree.isBlank() && folder.isNotBlank() && folder != DEFAULT_SAVE_FOLDER
    AlertDialog(onDismissRequest = onDismiss, title = { Text("选择${label}文件夹") }, shape = RoundedCornerShape(20.dp),
        text = { Column {
            @Composable fun option(selected: Boolean, title: String, detail: String, tag: String, onClick: () -> Unit) {
                Row(Modifier.fillMaxWidth().selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                    .padding(vertical = 8.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selected, onClick = null)
                    Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(title)
                        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            option(tree.isBlank() && folder.isBlank(), "系统目录", mediaLocationLabel(root, ""), "saveFolderSystem") { onPreset("") }
            option(tree.isBlank() && folder == DEFAULT_SAVE_FOLDER, DEFAULT_SAVE_FOLDER,
                mediaLocationLabel(root, DEFAULT_SAVE_FOLDER), "saveFolderDefault") { onPreset(DEFAULT_SAVE_FOLDER) }
            if(legacy) option(true, "之前填写的「$folder」", mediaLocationLabel(root, folder), "saveFolderLegacy") { onDismiss() }
            option(tree.isNotBlank(), "选择文件夹…",
                if(tree.isNotBlank()) SaveTree.label(tree) ?: "所选文件夹" else "用系统文件管理器挑选任意文件夹",
                "saveFolderPick", onPick)
        } },
        confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("关闭") } })
}

@Composable private fun FileNameTemplateCard(vm: MainViewModel) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf(vm.fileNameTemplate) }
    val previewName = remember(draft) {
        formatFileName(template = draft, title = "示例视频标题", id = "bv1xx411c7md", author = "作者名",
            platform = Platform.BILI, extension = "mp4")
    }
    UiGroup {
        SettingRow("文件命名规则", value = vm.fileNameTemplate, description = "预览：$previewName",
            onClick = { draft = vm.fileNameTemplate; editing = !editing }) {
            Glyph(if(editing) "close" else "chevron", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AnimatedVisibility(editing, enter = UiMotion.expand, exit = UiMotion.collapse) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("默认是日期加编号，不含标题。点预设换成整条规则，点下面的变量加进当前模板。动态照片保留 _MP 标记，图片自动追加序号。",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("预设规则", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NAMING_TEMPLATE_PRESETS.forEach { (tmpl, label) ->
                        FilterChip(selected = draft == tmpl, onClick = { draft = tmpl }, label = { Text(label) },
                            shape = RoundedCornerShape(10.dp))
                    }
                }
                Text("插入变量", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NAMING_INSERTS.forEach { (token, name) ->
                        FilterChip(selected = draft.contains(token), onClick = {
                            draft = if(draft.contains(token)) draft.replace(token, "") else (draft + token).take(80)
                        }, label = { Text(name) }, shape = RoundedCornerShape(10.dp))
                    }
                }
                OutlinedTextField(value = draft, onValueChange = { draft = it.take(80) }, label = { Text("自定义模板") },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("fileNameTemplateInput"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = { draft = vm.fileNameTemplate; editing = false }) { Text("取消") }
                    Button(onClick = { vm.changeFileNameTemplate(draft); editing = false }, shape = RoundedCornerShape(12.dp)) { Text("保存") }
                }
            }
        }
    }
}
