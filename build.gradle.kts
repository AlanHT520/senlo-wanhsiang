plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "net.alan.senlo"
version = "0.1.0"

repositories {
    mavenCentral()
}

javafx {
    version = "26"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml",
        "javafx.media",
        "javafx.web"
    )
}

application {
    mainClass = "net.alan.senlo.Main"
}

dependencies {
    // ========== 多媒体处理 ==========
    // FFmpeg Java 封装
    implementation("ws.schild:jave-all-deps:3.5.0")

    // 音频 SPI 扩展
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("javazoom:jlayer:1.0.1")
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7-1")

    // 图像处理
    implementation("org.apache.commons:commons-imaging:1.0.0-alpha6")

    // 压缩包处理
    implementation("org.apache.commons:commons-compress:1.28.0")
    implementation("net.sf.sevenzipjbinding:sevenzipjbinding:16.02-2.01")
    implementation("net.sf.sevenzipjbinding:sevenzipjbinding-all-platforms:16.02-2.01")

    // ========== 办公文档处理 ==========
    // Apache POI（Word、Excel、PowerPoint）
    implementation("org.apache.poi:poi-ooxml:5.4.0")
    implementation("org.apache.poi:poi-scratchpad:5.5.1")

    // PDF 处理
    implementation("org.apache.pdfbox:pdfbox:3.0.4")
    implementation("org.apache.pdfbox:pdfbox-tools:3.0.7")

    // Markdown 处理
    implementation("org.commonmark:commonmark:0.25.1")
    implementation("org.commonmark:commonmark-ext-gfm-tables:0.27.0")

    // ========== 辅助工具 ==========
    // 日志（SLF4J + Logback）
    implementation("org.slf4j:slf4j-api:2.0.18")
    implementation("ch.qos.logback:logback-classic:1.5.34")

    // JSON 处理
    implementation("com.google.code.gson:gson:2.13.2")

    // ========== 测试框架 ==========
    testImplementation(platform("org.junit:junit-bom:6.0.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

// 1. 配置主 JAR 的清单属性
tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Main-Class" to "net.alan.senlo.Main",
            // 告诉主 JAR 去同级或子目录下的 libs 文件夹里寻找依赖
            "Class-Path" to configurations.runtimeClasspath.get().files.joinToString(" ") { "libs/${it.name}" }
        )
    }
}

// 2. 创建一个新任务：自动将所有第三方依赖包拷贝到 build/libs/libs 目录下
val copyDependencies = tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("libs/libs"))
}

// 3. 让标准的 build 任务自动触发拷贝依赖的任务
tasks.named("build") {
    dependsOn(copyDependencies)
}