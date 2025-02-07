import java.io.*;
import java.net.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;

public class Client {
    private static final String SERVER_ADDRESS = "localhost"; // เปลี่ยนเป็นที่อยู่ IP ของเซิร์ฟเวอร์
    private static final int SERVER_PORT = 8080;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            // รับรายชื่อไฟล์จากเซิร์ฟเวอร์
            int fileCount = in.readInt();
            if (fileCount > 0) {
                System.out.println("Files available on the server:");
                for (int i = 0; i < fileCount; i++) {
                    System.out.println("- " + in.readUTF());
                }
            } else {
                System.out.println("No files available on the server.");
            }

            // เลือกวิธีการโอนถ่ายไฟล์
            System.out.print("TRANSFER METHOD (copy/zero): ");
            String transferMethod = reader.readLine();

            // ใส่ชื่อไฟล์ที่ต้องการดาวน์โหลด
            System.out.print("FILE NAME: ");
            String requestedFile = reader.readLine();
            out.writeUTF(transferMethod);
            out.writeUTF(requestedFile);

            // รับขนาดไฟล์และดาวน์โหลดไฟล์จาก Server
            long fileSize = in.readLong();
            if (fileSize > 0) {
                long startTime = System.currentTimeMillis();

                if ("zero".equalsIgnoreCase(transferMethod)) {
                    System.out.println("| ZERO |");
                    try (FileOutputStream fos = new FileOutputStream("D:\\OS Final\\client_files\\" + requestedFile);
                         FileChannel fileChannel = fos.getChannel();
                         ReadableByteChannel readableByteChannel = Channels.newChannel(in)) {
                        
                        long bytesTransferred = 0;
                        long position = 0;
                        long count;
                        
                        while (bytesTransferred < fileSize) {
                            count = fileChannel.transferFrom(readableByteChannel, position, fileSize - bytesTransferred);
                            if (count <= 0) break;
                            bytesTransferred += count;
                            position += count;
                        }
                        System.out.println("Download completed.");
                    }
                }
                else if ("copy".equalsIgnoreCase(transferMethod)) {
                    System.out.println("| COPY |");
                    try (FileOutputStream fos = new FileOutputStream("D:\\OS Final\\client_files\\" + requestedFile)) {
                        byte[] buffer = new byte[4096]; // Buffer ขนาด 4KB
                        long remaining = fileSize;
                        int read;
                        long bytesRead = 0;
                
                        // อ่านข้อมูลจากเซิร์ฟเวอร์และเขียนลงไฟล์
                        while (remaining > 0 && (read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining))) > 0) {
                            fos.write(buffer, 0, read);
                            bytesRead += read;
                            remaining -= read;
                
                            // คำนวณและแสดง Progress Bar
                            int progress = (int) ((bytesRead * 100) / fileSize);
                            System.out.print("\rDownloading: " + progress + "%");
                        }
                        System.out.println("\nDownload completed.");
                    }
                }
                long endTime = System.currentTimeMillis();
                System.out.println("Time taken for download: " + (endTime - startTime) + " ms");
            } else {
                System.out.println("File not found on server.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
