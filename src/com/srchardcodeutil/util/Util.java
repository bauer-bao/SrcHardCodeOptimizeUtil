package com.srchardcodeutil.util;

import com.intellij.notification.*;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Created by bauer on 2019/11/25.
 */
public class Util {

    /**
     * 保存字符串到文件
     *
     * @param path
     * @param content
     */
    public static void saveContentToFile(String path, String content) {
        try {
            OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8);
            BufferedWriter bw = new BufferedWriter(osw);
            bw.write(content);
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 保存到文件
     *
     * @param parentFile
     * @param sb
     */
    public static void saveToFile(VirtualFile parentFile, StringBuilder sb, boolean isString) {
        if (sb == null || sb.length() == 0) {
            return;
        }
        //获取父文件夹，找到string.xml，并且将字段添加到文件
        if (parentFile.getName().equalsIgnoreCase("res")) {
            //如果是res资源，则开始处理
            VirtualFile[] resDirChildren = parentFile.getChildren();
            boolean valuesExist = false;
            for (VirtualFile child : resDirChildren) {
                if (child.getName().equals("values") && child.isDirectory()) {
                    valuesExist = true;
                    //找到values目录，因为从layout中拿到的string，肯定只有一种语言，因此只在values中生成一份string.xml，不处理其他values文件夹
                    VirtualFile[] valuesChildren = child.getChildren();
                    //处理values文件夹
                    boolean exist = false;
                    for (VirtualFile value : valuesChildren) {
                        if (value.getName().equals(isString ? "strings.xml" : "colors.xml")) {
                            //找到strings.xml或者colors.xml
                            exist = true;
                            try {
                                //将文件转成string
                                String content = new String(value.contentsToByteArray(), StandardCharsets.UTF_8);
                                //替换成最新的string
                                String result = content.replace("</resources>", sb.toString() + "\n</resources>");
                                //将内容全部写入文件中
                                saveContentToFile(value.getPath(), result);
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            break;
                        }
                    }
                    if (!exist) {
                        //目标文件不存在，新建文件
                        File file1 = new File(child.getPath(), isString ? "strings.xml" : "colors.xml");
                        //生成内容
                        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n   " + sb.toString() + "\n</resources>";
                        //保存到文件
                        saveContentToFile(file1.getPath(), content);
                    }
                    break;
                }
            }
            if (!valuesExist) {
                //新建value文件夹
                File valueFile = new File(parentFile.getPath(), "values");
                valueFile.mkdirs();
                //新建xml文件
                File file1 = new File(valueFile.getPath(), isString ? "strings.xml" : "colors.xml");
                //生成内容
                String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n   " + sb.toString() + "\n</resources>";
                //保存到文件
                saveContentToFile(file1.getPath(), content);
            }
        }
    }

    /**
     * 方法目的：
     * 获取value对应的正确index
     * <p>
     * 弃用方案：
     * 1.如果用node转string的方案，则会改变原文件的原有代码
     * 2.如果直接用String.replace方法，查找的target可能在目标target前面，从而导致替换混乱
     * 3.如果使用正则，因为string的内容没法保证，可能存在和正则关键字冲突，工作量大，正则不易维护，最终放弃
     * <p>
     * 实现方案：
     * 1.查找当前value的索引值index
     * 2.检查索引值index之前几位字符串，是否包含"targetItem" + "="，如果包含，则返回当前index，如果不包含，则继续查找下一个index，直到找到为止
     *
     * @param content
     * @param value
     * @param targetItem
     * @param fromIndex
     * @return
     */
    public static int getRightIndex(StringBuilder content, String value, String targetItem, int fromIndex) {
        int index = content.indexOf("\"" + value + "\"", fromIndex);
        if (index == -1 || isRightIndex(content, targetItem, index - 1)) {
            //没有找到 或者正确的index，直接返回
            return index;
        } else {
            //继续查找下一个合适的index
            return getRightIndex(content, value, targetItem, index + value.length());
        }
    }

    /**
     * 检查index是否符合要求
     * <p>
     * 以android:text="aaa"为例。因为xml中属性标准写法如上，但是由于开发人员的不规范，=两边可能会有N个空格，需要过滤掉空格，但是:两边一定不会有空格，否则系统直接标红
     * 实现大概逻辑：对字符串从index开始，不断往前去循环，依次比对是否和targetItemStr一致，需要过滤空格。如果全部匹配，则符合要求
     *
     * @param content
     * @param targetItem
     * @param index
     * @return
     */
    private static boolean isRightIndex(StringBuilder content, String targetItem, int index) {
        //正常的属性类似android:text= 所以需要加上=
        String targetItemStr = targetItem + "=";
        int targetIndex = targetItemStr.length() - 1;
        boolean isRight = true;
        for (int i = index; i > 0; i--) {
            char cur = content.charAt(i);
            if (cur != ' ') {
                //不为空格
                if (cur == targetItemStr.charAt(targetIndex)) {
                    //继续匹配上一个值
                    targetIndex--;
                    if (targetIndex < 0) {
                        //全部匹配完
                        break;
                    }
                } else {
                    //不一致，则当前index不是正确的index
                    isRight = false;
                    break;
                }
            }
        }
        return isRight;
    }

    /**
     * 获取value的index
     * <p>
     * 以android:text="aaa"为例。因为xml中属性标准写法如上，但是由于开发人员的不规范，=两边可能会有N个空格，需要过滤掉空格，但是:两边一定不会有空格，否则系统直接标红
     * 实现大概逻辑：对字符串从index开始，不断往后去循环，依次比对是否和 =" 一致，需要过滤空格。如果全部匹配，则符合要求
     *
     * @param content
     * @param index
     * @return
     */
    public static int getValueIndex(String content, int index) {
        String targetItemStr = "=\"";
        int targetIndex = 0;
        int resultIndex = -1;
        for (int i = index; i < content.length(); i++) {
            char cur = content.charAt(i);
            if (cur != ' ') {
                //不为空格
                if (cur == targetItemStr.charAt(targetIndex)) {
                    //继续匹配下一个值
                    targetIndex++;
                    if (targetIndex >= targetItemStr.length()) {
                        //全部匹配完
                        resultIndex = i + 1;
                        break;
                    }
                } else {
                    //不一致
                    break;
                }
            }
        }
        return resultIndex;
    }

    /**
     * 提示error
     *
     * @param msg
     */
    public static void showError(String msg) {
        NotificationGroup notificationGroup = new NotificationGroup("Optimize", NotificationDisplayType.BALLOON, true);
        Notifications.Bus.notify(notificationGroup.createNotification(msg, NotificationType.ERROR));
    }

    /**
     * 提示信息
     *
     * @param msg
     */
    public static void showTip(String msg) {
        NotificationGroup notificationGroup = new NotificationGroup("Optimize", NotificationDisplayType.BALLOON, true);
        Notifications.Bus.notify(notificationGroup.createNotification(msg, NotificationType.INFORMATION));
    }
}