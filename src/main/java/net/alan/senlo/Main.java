package net.alan.senlo;

public class Main {
    public static void main(String[] args) {
        // 关键点：由一个普通的、不继承 Application 的类去间接调用 JavaFX 的 main
        MainFX.mainfx(args);
    }
}