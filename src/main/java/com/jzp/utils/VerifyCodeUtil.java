package com.jzp.utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * FileName:    CheckCodeUtil
 * Author:      jzp
 * Date:        2020/6/16 14:36
 * Description: 验证码工具类
 */
public class VerifyCodeUtil {

    /**
     * 验证码图片的宽度
     */
    private static final int WIDTH = 90;
    /**
     * 验证码图片的高度
     */
    private static final int HEIGHT = 20;
    /**
     * 验证码中字符的个数
     */
    private static final int CODE_COUNT = 4;
    /**
     * X 轴坐标
     */
    private static final int X_POSITION = 15;
    /**
     * Y 轴坐标
     */
    private static final int Y_POSITION = 16;
    /**
     * 字体大小
     */
    private static final int FONT_HEIGHT = 18;
    /**
     * 字符序列
     */
    private static final char[] CODE_SEQUENCE = {
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I',
            'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R',
            'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0',
            '1', '2', '3', '4', '5', '6', '7', '8', '9'
    };

    /**
     * 生成一个 Map 集合
     * - code 为生成的验证码
     * - codePic 为生成的验证码BufferedImage对象
     */
    public static Map<String, Object> generateCodeAndPicture() {
        /* 定义图像 buffer，并使用 buffer 生成 Graphics */
        BufferedImage bufferedImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics graphics = bufferedImage.getGraphics();

        /* 将图像填充为白色 */
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, WIDTH, HEIGHT);

        /* 创建字体，字体的大小应该根据图片的高度来定，并将字体设置到 graphics 中 */
        Font font = new Font("Fixedsys", Font.BOLD, FONT_HEIGHT);
        graphics.setFont(font);

        /* 画边框 */
        graphics.setColor(Color.BLACK);
        graphics.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

        /* 使用随机数生成器随机产生 40 条干扰线，使图象中的认证码不易被其它程序探测到 */
        Random random = new Random();
        graphics.setColor(Color.BLACK);
        for (int i = 0; i < 30; i++) {
            /* 起点的 X 和 Y 轴坐标 */
            int x = random.nextInt(WIDTH), y = random.nextInt(HEIGHT);
            /* X 轴方向和 Y 轴方向的长度 */
            int xLength = random.nextInt(12), yLength = random.nextInt(12);
            /* 画线 */
            graphics.drawLine(x, y, x + xLength, y + yLength);
        }

        /* 使用 randomCode 用于保存随机产生的验证码，以便用户登录后进行验证 */
        StringBuffer randomCode = new StringBuffer();
        int red, green, blue;
        /* 随机产生由 CODE_COUNT 指定字符个数的验证码 */
        for (int i = 0; i < CODE_COUNT; i++) {
            /* 得到随机产生的验证码数字 */
            String current = String.valueOf(CODE_SEQUENCE[random.nextInt(36)]);
            /* 产生随机的颜色分量来构造颜色值，这样输出的每位数字的颜色值都将不同 */
            red = random.nextInt(255);
            green = random.nextInt(255);
            blue = random.nextInt(255);
            /* 用随机产生的颜色将验证码绘制到图像中 */
            graphics.setColor(new Color(red, green, blue));
            graphics.drawString(current, (i + 1) * X_POSITION, Y_POSITION);
            /* 将产生的四个随机数组合在一起 */
            randomCode.append(current);
        }

        /* 将 code 和 codePicture 存入 Map 并返回 */
        Map<String, Object> map = new HashMap<>();
        map.put("code", randomCode);
        map.put("codePicture", bufferedImage);
        return map;
    }
}
