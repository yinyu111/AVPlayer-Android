package com.kaku.avplayer.Base;

public class test {
    // 定义一个私有属性
    private String message;

    // 构造方法，用于初始化属性
    public test(String message) {
        this.message = message;
    }

    // 定义一个公共方法，用于输出信息
    public void displayMessage() {
        System.out.println("Message: " + message);
    }

    public static void main(String[] args) {
        // 创建 test 类的对象
        test example = new test("Hello, this is a test!");
        // 调用 displayMessage 方法
        example.displayMessage();
        example.displayMessage();
    }
}
