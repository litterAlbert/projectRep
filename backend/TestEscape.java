public class TestEscape {
    public static void main(String[] args) {
        String url = "https://oss-cn-hangzhou.aliyuncs.com/test.pdf";
        // 也可以直接去掉标点
        System.out.println("ESCAPED: " + url.replaceAll("[^a-zA-Z0-9]", ""));
    }
}