package com.company;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.StringTokenizer;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author jxc
 * 一个进程对应可以有多个线程,采用多线程服务器实现，
 * 一是在连接过多的时候使得已经连接的请求可以传递消息，
 * 尤其是I/O设备读写的时候提高效率
 * 二十防止在只有主线程的情况下只有一个线线程出现网络延迟或者阻塞而队列中所有进程都进入死锁
 */
public class TestSocketHttp{
    private static final int PORT = 8089;

    public static void main(String[] args) {
        try{
            ServerSocket serverSocket = new ServerSocket(PORT);
            while(true){
                try{
                    Socket connection = serverSocket.accept();
                    Thread task = new HttpSocket(connection);
                    task.start();
                    System.out.println("HTTP服务器正在运行,端口：" + PORT);
                }catch(Exception ex){

                }
            }
        }catch(Exception ex){
            System.exit(0);
        }

    }


}

class Request{
    private String uri;//资源路径
    private InputStream inputStream;//读取请求
    private String encoding = "GBK";//请求资源的编码

    Request(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public String getEncoding() {
        return encoding;
    }

    public String getUri() {
        return uri;
    }

    public void parse() throws IOException{
        System.out.println("客户端发送的信息：------------------>");
        //读取第一行，请求地址
        String line = readLine(inputStream, 0);
        //打印首部行
        System.out.print(line);

        //获取资源的路径
        uri = line.substring(line.indexOf('/'),line.lastIndexOf('/') - 5);
        //获取请求方法
        String method = new StringTokenizer(line).nextElement().toString();
        //如果是POST方法，则会有消息体长度
        int contentLength = 0;

        //读取包含元数据的HTTP首部并打印
        do{
            line = readLine(inputStream, 0);
            //如果有Content-Length消息头时输出
            if(line.startsWith("Content-Length")){
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            }
            //打印元数据信息
            System.out.print(line);
            //如果遇到了一个单独的回车换行，则表示请求头结束
        }while(!line.equals("\r\n"));

        //如果是Post请求,则有消息体
        if("POST".equalsIgnoreCase(method)){
            //这里只是简单的处理表单提交的参数，而对于上传文件这里是不能这样处理的
            //因为上传的文件时消息体不止一行，会有多行消息体
            System.out.print(readLine(inputStream, contentLength));
        }

        //客户端发送消息结束
        System.out.println("客户端发送请求消息结束！！！！！！！！！！！！！！！");
        System.out.println("用户请求的资源是：" + uri);
        System.out.println("用户请求的方法类型是： " + method);
    }
    /**
     *
     * 这里模拟读取一行，因为如果使用API中的BufferedReader时，它是读取到一个回车换行后才返回，
     * 否则如果没有读取，则一直阻塞，这就导致如果POST请求时，表单中的元素以消息体传送，这时，消息体最末
     * 按标准是没有回车换行的，如果此时还使用BufferedReader来读取时，则POST提交时会阻塞。如果是POST
     * 提交时我们按照消息体的长度Context-Length来截取消息体，这样不会被阻塞。
     */
    private String readLine(InputStream inputStream, int contentLength) throws IOException{
        ArrayList<Object> arrayList = new ArrayList<>();
        byte readByte = 0;
        int total = 0;
        if(contentLength != 0){//post请求
            while(total < contentLength){
                readByte = (byte)inputStream.read();
                arrayList.add(Byte.valueOf(readByte));
                total++;
            }
        }else{//get请求
            while(readByte != 10){
                readByte = (byte)inputStream.read();
                arrayList.add(Byte.valueOf(readByte));
            }
        }

        byte[] tempByteArr = new byte[arrayList.size()];
        for(int i = 0; i < arrayList.size(); i++){
            tempByteArr[i] = ((Byte)arrayList.get(i)).byteValue();
        }
        arrayList.clear();

        String tempStr = new String(tempByteArr,encoding);
        /**
         * HTTP请求中的header中有一个referer属性，这个属性的意思就是如果当前请求是从别的页面链接过来的，
         * 那个属性就是那个页面的url，如果请求的url是直接从浏览器地址栏输入的就没有这个值。得到这个值可以实现
         * 很多有用的功能，例如防盗链，记录访问来源以及记住刚才访问的链接等。另外，从浏览器发送这个Referer
         * 链接时好像固定用utf-8编码的，所以在GBK下出现乱码，这里纠正一下
         */
        if(tempStr.startsWith("Referer")){//如果有Referer头时，使用UTF-8编码
            tempStr = new String(tempByteArr,"UTF-8");
        }
        return tempStr;

    }

}

class Response{
    private Request request;//用于读取资源的uri
    private OutputStream outputStream;//用于输出资源

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public void sentResponse() throws IOException {
        PrintWriter out = new PrintWriter(outputStream);
        FileInputStream fileInputStream = new FileInputStream("/Users/zhaowenyue/Desktop/McuSDK_WinJava/release/Demo/socketserver3/picture/2.jpg");
        byte[] bytes = new byte[fileInputStream.available()];
        fileInputStream.read(bytes);

        String base64 = Base64.encodeBase64String(bytes);
        System.out.println("base64 = " + base64);

        out.println("<img src=\"data:image/jpeg;base64," +base64+ "\"/>");
        out.println("< mg src=\"data:image/jpeg;base64," + base64 + "\"/>");

//        PrintWriter out = new PrintWriter(outputStream);
//        //返回一个状态行
//        out.println("HTTP/1.0 200 OK");
//        //返回一个首部
//        out.println("Content-Type:text/html;charset=" + request.getEncoding());
//        // 根据 HTTP 协议, 空行将结束头信息
//        out.println();
//
//        // 输出请求资源

//        out.close();
        fileInputStream.close();
        out.close();

    }

}

class HttpSocket extends Thread{
    private Socket connection;//客户端与服务器之间的连接

    public HttpSocket(Socket connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        if(connection != null){
            System.out.println("线程：" + Thread.currentThread().getName());
            System.out.println("连接到服务器的用户：" + connection);
            try{
                Request request = new Request(connection.getInputStream());
                request.parse();
                Response response = new Response();
                response.setRequest(request);
                response.setOutputStream(connection.getOutputStream());
                response.sentResponse();

                //因为http1.0是无状态协议，所以必须关闭连接
                closeSocket(connection);
            }catch(Exception ex){
                System.out.println("HTTP服务器错误：" + ex.getLocalizedMessage());
            }
        }
    }

    /**
     * 关闭客户端 socket 并打印一条调试信息.
     * @param socket 客户端 socket.
     */
    void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        System.out.println(socket + "离开了HTTP服务器");
    }


}