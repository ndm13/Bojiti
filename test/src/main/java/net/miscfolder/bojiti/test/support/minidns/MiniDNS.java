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

	private static final DnsClient SYSTEM_CLIENT = new DnsClient();
	private static final AbstractDnsClient SAFE_CLIENT = new OpenNICDnsClient();
	private static volatile boolean useSafeClient = false;
	static{
		DnsClient.addDnsServerLookupMechanism(new WindowsNTDnsLookup(1000));
		SYSTEM_CLIENT.setDisableResultFilter(true);
		SYSTEM_CLIENT.setPreferedIpVersion(AbstractDnsClient.IpVersionSetting.v4only);
		SYSTEM_CLIENT.setAskForDnssec(true);
		SAFE_CLIENT.setPreferedIpVersion(AbstractDnsClient.IpVersionSetting.v4only);
	}
	public static boolean isValidRoot(String root) throws IOException{
		if(!root.endsWith(".")) root += ".";
		return checkQuestion(new Question(root, Record.TYPE.ANY, Record.CLASS.ANY));
	}

	public static boolean isValidInternet(String host) throws IOException{
		return checkQuestion(new Question(host, Record.TYPE.ANY, Record.CLASS.IN));
	}

	private static boolean checkQuestion(Question question) throws IOException{
		DnsMessage response = (useSafeClient ? SAFE_CLIENT : SYSTEM_CLIENT).query(question);
		if(response == null) return false;
		if(response.responseCode.equals(DnsMessage.RESPONSE_CODE.BADNAME)) return false;
		if(!useSafeClient && response.responseCode.equals(DnsMessage.RESPONSE_CODE.SERVER_FAIL)){
			useSafeClient = true;
			return checkQuestion(question);
		}
		Set<Data> answers = response.getAnswersFor(question);
		return !(answers == null || answers.isEmpty());
	}
}
