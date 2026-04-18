package com.morunesocketing;

import java.io.*;
import java.util.regex.Pattern;

/**
 * 版本管理器
 * 实现版本号自动递增：每10个小版本进一个中版本，每10个中版本进一个大版本
 */
public class VersionManager {
    
    private static final String VERSION_FILE = "version.txt";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
    
    /**
     * 获取当前版本号
     */
    public static String getCurrentVersion() {
        try {
            File versionFile = new File(VERSION_FILE);
            if (!versionFile.exists()) {
                // 如果版本文件不存在，创建并初始化为1.0.0
                return createInitialVersion();
            }
            
            try (BufferedReader reader = new BufferedReader(new FileReader(versionFile))) {
                String version = reader.readLine();
                if (version != null && VERSION_PATTERN.matcher(version).matches()) {
                    return version;
                } else {
                    // 如果版本格式不正确，重置为1.0.0
                    return createInitialVersion();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return "1.0.0";
        }
    }
    
    /**
     * 递增版本号
     */
    public static String incrementVersion() {
        String currentVersion = getCurrentVersion();
        String[] parts = currentVersion.split("\\.");
        
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        int patch = Integer.parseInt(parts[2]);
        
        // 递增小版本号
        patch++;
        
        // 检查是否需要进位
        if (patch >= 10) {
            patch = 0;
            minor++;
            
            if (minor >= 10) {
                minor = 0;
                major++;
            }
        }
        
        String newVersion = major + "." + minor + "." + patch;
        
        // 保存新版本号
        saveVersion(newVersion);
        
        return newVersion;
    }
    
    /**
     * 创建初始版本文件
     */
    private static String createInitialVersion() {
        String initialVersion = "1.0.0";
        saveVersion(initialVersion);
        return initialVersion;
    }
    
    /**
     * 保存版本号到文件
     */
    private static void saveVersion(String version) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(VERSION_FILE))) {
            writer.write(version);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 更新pom.xml中的版本号
     */
    public static void updatePomVersion() {
        try {
            String newVersion = incrementVersion();
            File pomFile = new File("pom.xml");
            
            if (!pomFile.exists()) {
                System.err.println("pom.xml文件不存在！");
                return;
            }
            
            // 读取pom.xml内容
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(pomFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 替换版本号
                    if (line.contains("<version>")) {
                        line = line.replaceAll("<version>[^<]+</version>", "<version>" + newVersion + "</version>");
                    }
                    content.append(line).append("\n");
                }
            }
            
            // 写回pom.xml
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(pomFile))) {
                writer.write(content.toString());
            }
            
            System.out.println("版本号已更新为: " + newVersion);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 主方法 - 用于手动更新版本号
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("increment")) {
            updatePomVersion();
        } else {
            System.out.println("当前版本: " + getCurrentVersion());
            System.out.println("使用方法: java VersionManager increment");
        }
    }
}