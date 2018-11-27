package net.miscfolder.bojiti.test.support.minidns;

import org.minidns.AbstractDnsClient;
import org.minidns.DnsClient;
import org.minidns.dnsmessage.DnsMessage;
import org.minidns.dnsmessage.Question;
import org.minidns.record.Data;
import org.minidns.record.Record;

import java.io.IOException;
import java.util.Set;

public class MiniDNS{
	public static void main(String[] args) throws IOException{
		System.out.println(isValidRoot("com."));
		System.out.println(isValidRoot("html."));
	}

	private static final DnsClient CLIENT = new DnsClient();
	static{
		DnsClient.addDnsServerLookupMechanism(new WindowsNTDnsLookup(1000));
		CLIENT.setPreferedIpVersion(AbstractDnsClient.IpVersionSetting.v6v4);
		CLIENT.setAskForDnssec(true);
	}
	public static boolean isValidRoot(String root) throws IOException{
		if(!root.endsWith(".")) root += ".";
		return checkQuestion(new Question(root, Record.TYPE.ANY, Record.CLASS.ANY));
	}

	public static boolean isValidInternet(String host) throws IOException{
		return checkQuestion(new Question(host, Record.TYPE.ANY, Record.CLASS.IN));
	}

	private static boolean checkQuestion(Question question) throws IOException{
		DnsMessage response = CLIENT.query(question);
		if(response == null) return false;
		if(response.responseCode.equals(DnsMessage.RESPONSE_CODE.BADNAME)) return false;
		Set<Data> answers = response.getAnswersFor(question);
		return !(answers == null || answers.isEmpty());
	}
}
