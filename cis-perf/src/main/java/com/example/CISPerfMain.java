package com.example;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CISPerfMain implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(CISPerfMain.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		
		int count = 3;
		String deploy = "deploy.yaml";
		String configmap = "configmap.yaml";
		String backend = "kylinsoong/backend:0.0.4";
		String net = "10.1.10.0/24";
		int ip_start = 3;
		
		if(args.length < 2) {
			StringBuffer sb = new StringBuffer();
			sb.append("Invalid parameters").append("\n");
			sb.append("Run with: --count <COUNT> --deploy <DEPLOYMENT NAME> --configmap <CONFIGMAP NAME> --backend <IMAGE NAME> --net <VS ADDR> --ipstart <IP START>").append("\n");
			sb.append("      eg: --count 20 --deploy deploy.yaml --configmap configmap.yaml --backend 'kylinsoong/backend:0.0.4' --net '10.1.10.0/24' --ipstart 3");
			throw new RuntimeException(sb.toString());
		}
		
		for(int i = 0 ; i < args.length ; i++) {
			
			if(args[i].equals("--count")) {
				count = Integer.parseInt(args[++i]);
			} else if(args[i].equals("--deploy")) {
				deploy = args[++i];
			} else if(args[i].equals("--configmap")) {
				configmap = args[++i];
			} else if(args[i].equals("--backend")) {
				backend = args[++i];
			} else if(args[i].equals("--net")) {
				net = args[++i];
			} else if(args[i].equals("--ipstart")) {
				ip_start = Integer.parseInt(args[++i]);
			}

		}
		
		
		
		if(!net.endsWith(".0/24")) {
			throw new RuntimeException("Current only support /24 network");
		}
		
		String net_prefix = net.substring(0, net.length() - 5);
				
		int start = 100;
		boolean first = true;
		boolean firstCM = true;
		
		String cmStart = getResourceFileAsString("cm.start");
		StringBuffer sb = new StringBuffer();
		sb.append(cmStart);
				
		System.out.println("Generating K8S deployments to " + deploy);
		for (int i = 0 ; i < count ; i ++) {
			
			String raw = getResourceFileAsString("deploy.yaml");
			String ns = "cistest" + String.valueOf(start + i);
						
			raw = raw.replaceAll("REPLACEMENT_NAMESPACE", ns);
			raw = raw.replaceAll("REPLACEMENT_BACKEND_IMAGE", backend);
			
			if(first) {
				first = false;
				if (Files.exists(Paths.get(deploy))) {
					Files.delete(Paths.get(deploy));
				}
				Files.createFile(Paths.get(deploy));
				
				if (Files.exists(Paths.get(configmap))) {
					Files.delete(Paths.get(configmap));
				}
				Files.createFile(Paths.get(configmap));
			} else {
				Files.write(Paths.get(deploy), "---\n".getBytes(), StandardOpenOption.APPEND);
			}
			
			Files.write(Paths.get(deploy), raw.getBytes(), StandardOpenOption.APPEND);
			Files.write(Paths.get(deploy), "\n".getBytes(), StandardOpenOption.APPEND);
			
			// configmap
			
			String cm = getResourceFileAsString("cm.content");
			cm = cm.replaceAll("REPLACEMENT_NAMESPACE", ns);
			String vip = net_prefix + "." + (i + ip_start);
			cm = cm.replaceAll("REPLACEMENT_BIGIP_VS_IP_ADDR", vip);
			if(firstCM) {
				firstCM = false;
				sb.append("\n").append(cm);
			} else {
				sb.append(",").append("\n").append(cm);
			}
			
		}
		
		System.out.println("Generating AS3 configmap to " + configmap);
		
		String cmEnd = getResourceFileAsString("cm.end");
		sb.append("\n").append(cmEnd);
		Files.write(Paths.get(configmap), sb.toString().getBytes());
		
		
		
	}
	
	static String getResourceFileAsString(String fileName) throws IOException {
		
		
		String content = Files.readString(Paths.get("template", fileName), StandardCharsets.US_ASCII);
		
		if(content == null || content.length() == 0) {
			throw new RuntimeException(fileName + " not exist under template");
		}
	    
		return content;
	}
	
	void tmp(String... args) {
		
		if(args.length != 2) {
			System.out.println("Run with paramters: java -jar http-client.jar <IP> <Port> <Times>");
			System.out.println("                    <IP> - Server IP address");
			System.out.println("                    <Port> - Service Port, like 80, 443, 8080, 8443, etc");
			System.exit(0);
		}
		
		String ip = args[0];
		int port = Integer.parseInt(args[1]);
		
		System.out.println("Send 3 http request without GET");
	}

	
}
