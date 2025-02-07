import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.*;

@SuppressWarnings("unused")
public class Server {
    private static final int SERVER_PORT = 8080;
    private static final String FILE_DIRECTORY = "D:\\OS Final\\server_files\\"; // ระบุไดเรกทอรีที่เก็บไฟล์

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            System.out.println("Server is running on port " + SERVER_PORT + "\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket clientSocket;
    
        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }
    
        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                 DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
                
                // ส่งรายชื่อไฟล์ทั้งหมดในไดเรกทอรี
                File folder = new File(FILE_DIRECTORY);
                File[] files = folder.listFiles();
                if (files != null) {
                    out.writeInt(files.length);
                    for (File file : files) {
                        out.writeUTF(file.getName());
                    }
                } else {
                    out.writeInt(0);
                }
    
                // รับวิธีการโอนถ่ายไฟล์และชื่อไฟล์ที่ต้องการ
                String transferMethod = in.readUTF();
                String requestedFile = in.readUTF();
                File fileToSend = new File(FILE_DIRECTORY + requestedFile);
    
                if (fileToSend.exists()) {
                    out.writeLong(fileToSend.length());  // ส่งขนาดไฟล์ไปยังไคลเอนต์
    
                    // บันทึกเวลาเริ่มต้น
                    long startTime = System.currentTimeMillis();
    
                    // ส่งไฟล์
                    if ("copy".equalsIgnoreCase(transferMethod)) {
                        System.out.println("(COPY Method)");
                        try (FileInputStream fileIn = new FileInputStream(fileToSend)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = fileIn.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                        }
                    } else if ("zero".equalsIgnoreCase(transferMethod)) {
                        // ใช้การส่งแบบ zero-copy (ถ้าภาษารองรับ)
                        // ใน Java, อาจจำลองด้วยการส่งผ่าน `FileChannel` เพื่อเลียนแบบการส่งแบบ zero-copy
                        System.out.println("(ZERO Method)");
                        try (@SuppressWarnings("resource")
                        FileChannel fileChannel = new FileInputStream(fileToSend).getChannel()) {
                            fileChannel.transferTo(0, fileChannel.size(), Channels.newChannel(out));
                        }
                    }
                    // บันทึกเวลาสิ้นสุด
                    long endTime = System.currentTimeMillis();
    
                    // คำนวณเวลาในการดาวน์โหลดและแสดงผล
                    long timeTaken = endTime - startTime;
                    System.out.println("Time taken for download: "
                    + timeTaken + " ms\n-------------------------------------------");
    
                } else {
                    System.out.println("Requested file not found: " 
                    + requestedFile + "\n-------------------------------------------");
                    out.writeLong(-1);  // แจ้งเตือนว่าไฟล์ไม่พบ
                }
    
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
