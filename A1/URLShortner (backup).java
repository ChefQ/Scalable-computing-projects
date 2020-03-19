import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.RandomAccessFile;

public class URLShortner { 
	
	static final File WEB_ROOT = new File(".");
	static final String DEFAULT_FILE = "index.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String METHOD_NOT_SUPPORTED = "not_supported.html";
	static final String REDIRECT_RECORDED = "redirect_recorded.html";
	static final String REDIRECT = "redirect.html";
	static final String NOT_FOUND = "notfound.html";
	static final String DATABASE = "/virtual/database.txt";

	static final Pattern space = Pattern.compile("^@+$");

	static int PORT;

	public static volatile ConcurrentHashMap<String,String> cache = new ConcurrentHashMap<String,String>();
	
	// verbose mode
	static final boolean verbose = false;

	public static void main(String[] args) {
		PORT = Integer.parseInt(args[0]);
		try {
			ServerSocket serverConnect = new ServerSocket(PORT);
			System.out.println("Server started for clients.\nListening for connections on port : " + PORT + " ...\n");
			int numThreads = 0;
			Thread [] threadArray = new Thread[4];
			int turn = 0;

			ServerSocket serverMonitor = new ServerSocket(PORT+3);

			Thread monitorThread = new Thread() {
				public void run() {
				System.out.println("Server started for monitor.\nListening for connections on port : " + PORT+3 + " ...\n");
				handleMonitor(serverMonitor);
				}
			};
			monitorThread.start();
			
			// we listen until user halts server execution
			while (true) {
				if (verbose) { System.out.println("Connecton opened. (" + new Date() + ")"); }
				final Socket client = serverConnect.accept();
				System.out.println("Current map: " + cache);
				Thread t = new Thread(new Runnable() {
					public void run(){
						System.out.println("Made a new thread");
						handle(client);
					}
				});
				try {
					if(numThreads < 4){
						threadArray[numThreads] = t;
						threadArray[numThreads].start();
						numThreads += 1;
					}
					else {
						threadArray[turn].join();
						threadArray[turn] = t;
						threadArray[turn].start();
						turn = (turn + 1)%4;
					}
				} catch(InterruptedException e) {
					System.err.println(e);
				}
				//handle(client); // One thread only
			}
		} catch (IOException e) {
			System.err.println("Server Connection error : " + e.getMessage());
		}
	}

	public static void handle(Socket connect) {
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		
		try {
			in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
			out = new PrintWriter(connect.getOutputStream());
			dataOut = new BufferedOutputStream(connect.getOutputStream());
			
			String input = in.readLine();

			System.out.println("Input: " + input);
			
			if(verbose)System.out.println("first line: "+input);
			Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
			Matcher mput = pput.matcher(input);
			if(mput.matches() && mput.group(1).length() <= 24){
				String shortResource=mput.group(1);
				String longResource=mput.group(2);
				String httpVersion=mput.group(3);

				save(shortResource, longResource);

				File file = new File(WEB_ROOT, REDIRECT_RECORDED);
				int fileLength = (int) file.length();
				String contentMimeType = "text/html";
				//read content to return to client
				byte[] fileData = readFileData(file, fileLength);
					
				out.println("HTTP/1.1 200 OK");
				out.println("Server: Java HTTP Server/Shortner : 1.0");
				out.println("Date: " + new Date());
				out.println("Content-type: " + contentMimeType);
				out.println("Content-length: " + fileLength);
				out.println(); 
				out.flush(); 

				dataOut.write(fileData, 0, fileLength);
				dataOut.flush();
			} else {
				Pattern pget = Pattern.compile("^(\\S+)\\s+/(\\S+)\\s+(\\S+)$");
				Matcher mget = pget.matcher(input);
				if(mget.matches()){
					String method=mget.group(1);
					String shortResource=mget.group(2);
					String httpVersion=mget.group(3);

					String longResource = find(shortResource);
					if(longResource!=null){
						File file = new File(WEB_ROOT, REDIRECT);
						int fileLength = (int) file.length();
						String contentMimeType = "text/html";
	
						//read content to return to client
						byte[] fileData = readFileData(file, fileLength);
						System.out.println("Sending back redirect");
						
						// out.println("HTTP/1.1 301 Moved Permanently");
						out.println("HTTP/1.1 307 Temporary Redirect");
						out.println("Location: "+longResource);
						out.println("Server: Java HTTP Server/Shortner : 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + contentMimeType);
						out.println("Content-length: " + fileLength);
						out.println(); 
						out.flush(); 
	
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					} else {
						File file = new File(WEB_ROOT, FILE_NOT_FOUND);
						int fileLength = (int) file.length();
						String content = "text/html";
						byte[] fileData = readFileData(file, fileLength);
						
						out.println("HTTP/1.1 404 File Not Found");
						out.println("Server: Java HTTP Server/Shortner : 1.0");
						out.println("Date: " + new Date());
						out.println("Content-type: " + content);
						out.println("Content-length: " + fileLength);
						out.println(); 
						out.flush(); 
						
						dataOut.write(fileData, 0, fileLength);
						dataOut.flush();
					}
				}
				
				else {
					File file = new File(WEB_ROOT, DEFAULT_FILE);
					int fileLength = (int) file.length();
					String contentMimeType = "text/html";

					//read content to return to client
					byte[] fileData = readFileData(file, fileLength);
					
					// out.println("HTTP/1.1 301 Moved Permanently");
					out.println("HTTP/1.1 200 Success");
					out.println("Server: Java HTTP Server/Shortner : 1.0");
					out.println("Date: " + new Date());
					out.println("Content-type: " + contentMimeType);
					out.println("Content-length: " + fileLength);
					out.println(); 
					out.flush(); 

					dataOut.write(fileData, 0, fileLength);
					dataOut.flush();
				}
				
			}
		} catch (Exception e) {
			System.err.println("Server error:" + e.getMessage());
			System.err.println(e.getStackTrace());

		} finally {
			try {
				in.close();
				out.close();
				System.out.println("Closing client");
				connect.close(); // we close socket connection
			} catch (Exception e) {
				System.err.println("Error closing stream : " + e.getMessage());
			} 
			
			if (verbose) {
				System.out.println("Connection closed.\n");
			}
		}
	}

	private static String find(String shortURL){
		String longURL = null;

		/*
		System.out.println("Cache contains url? " + cache.contains(shortURL));
		System.out.println(cache);
		System.out.println(URLShortner.cache);
		*/

		// Check if it's in the cache
		if(cache.get(shortURL) != null) {
			System.out.println("Getting it out of cache.");
			return cache.get(shortURL);
		}

		try {
			RandomAccessFile f = new RandomAccessFile(DATABASE,"rw");
			int index = Hash.hasher(shortURL);

      		f.seek(145*index);
			byte[] entry = new byte[24];
			f.read(entry);
			String map = new String(entry);
			if(map.equals(shortURL+"@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length()))){
				byte[] longentry = new byte[120];
				f.read();
				f.read(longentry);
				String longmap = new String(longentry);

				System.out.println("Storing it in cache.");
				cache.putIfAbsent(shortURL, longmap.replace("@",""));

				return longmap.replace("@","");
			}else if(map.equals(shortURL+"@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length()))==false){
				byte[] longentry = new byte[120];
				f.read();
				f.read(longentry);
				String longmap = new String(longentry);
				Matcher m = space.matcher(longmap);
				if(m.matches()){
					return "";
				}else{
					for(int i=0;i<=100000;i++) {
						f.seek(145*((index + ((int)Math.pow(i, 2)) +i)%972801));
						entry = new byte[24];
						f.read(entry);
						map = new String(entry);
						if(map.equals(shortURL+"@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length()))){
							longentry = new byte[120];
							f.read();
							f.read(longentry);
							longmap = new String(longentry);
							return longmap.replace("@","");
						}else if(map.equals(shortURL+"@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length()))==false){
							longentry = new byte[120];
							f.read();
							f.read(longentry);
							longmap = new String(longentry);
							m = space.matcher(map);
							if(m.matches()){
								break;
							}
						}
					}
				}
			}
			
		} catch (IOException e) {
			
		} 
		return "";
	}

	private static synchronized void save(String shortURL,String longURL){

		String myShortURL = shortURL;

		try {
			RandomAccessFile f = new RandomAccessFile(DATABASE,"rw");
			int index = Hash.hasher(shortURL);
			System.out.println("index = " + index);

      		f.seek(145*index);
			byte[] entry = new byte[24];
			f.read(entry);
			String map = new String(entry);
			System.out.println("map[0] = " + map);
			if(map.equals(shortURL+"@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length()))){

			}else if(map.equals(shortURL+"@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length()))==false){
				System.out.println("not in the file");
				byte[] longentry = new byte[120];
				f.read();
				f.read(longentry);
				String longmap = new String(longentry);
				System.out.println(longmap);
				Matcher m = space.matcher(longmap);
				System.out.println("m = " + m);
				if(m.matches()){
					System.out.println("it matched");
					f.seek(145*index);
					shortURL = shortURL + "@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length());
					System.out.println(longURL);
      					f.writeBytes(shortURL + "\t" + longURL);
						f.close();
						System.out.println("Storing it in cache.");
						cache.putIfAbsent(myShortURL, longURL);  
				}else{
				for(int i=0;i<=100000;i++) {
					f.seek(145*((index+ ((int)Math.pow(i, 2)) +i)%972801));
					entry = new byte[24];
					f.read(entry);
					map = new String(entry);
					if(map.equals(shortURL+"@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length()))){
						break;
					}else if(map.equals(shortURL+"@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length()))==false){
						longentry = new byte[120];
						f.read();
						f.read(longentry);
						longmap = new String(longentry);
						m = space.matcher(map);
						if(m.matches()){
							f.seek(145*((index+ ((int)Math.pow(i, 2)) +i)%972801));
							shortURL = shortURL + "@@@@@@@@@@@@@@@@@@@@@@@@".substring(shortURL.length());
      							f.writeBytes(shortURL + "\t" +longURL);
								f.close();
								System.out.println("Storing it in cache.");
								cache.putIfAbsent(myShortURL, longURL);  
						}
					}
				}
			}
		}
			
		} catch (IOException e) {
			
		} 
		return;

	}
	
	private static byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];
		
		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null) 
				fileIn.close();
		}
		
		return fileData;
	}
	public static void handleMonitor(ServerSocket serverMonitor) {
		BufferedReader in = null; PrintWriter out = null; BufferedOutputStream dataOut = null;
		Socket connect = null;
		while(true){
			try {
				System.out.println("Listening for monitor signal");
				connect = serverMonitor.accept();

				in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
				out = new PrintWriter(connect.getOutputStream());
				dataOut = new BufferedOutputStream(connect.getOutputStream());
				
				String input = in.readLine();

				System.out.println("Input: " + input);
				
				// signals from monitor
				if(input.equals("ping")){
					out.println("on");
					out.flush();
					dataOut.flush();	
				}

			} catch (IOException e) {
				System.err.println("Server error:" + e.getMessage());
				System.err.println(e.getStackTrace());
			} finally {
				try {
					in.close();
					out.close();
					connect.close(); // we close socket connection
				} catch (Exception e) {
					System.err.println("Error closing stream : " + e.getMessage());
				} 
			}
		}
	}
}
