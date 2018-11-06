package net.miscfolder.bojiti.parser.rfcmessage;

import net.miscfolder.bojiti.parser.ParserException;
import net.miscfolder.protopack.ProtoPack;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class RFC822ParserTest{
	private static final String message = "\n" +
			"Date: Thu, 11 Mar 93 15:08:26 CST\n" +
			"Message-Id: <9303112108.AA10984@boombox.micro.umn.edu>\n" +
			"Received: from rawBits.micro.umn.edu by boombox.micro.umn.edu; Thu, 11 Mar 93 15:08:26 CST\n" +
			"From: \"The Minnesota Gopher Team\"  <gopher>\n" +
			"To: pacs-l@uhupvm1.uh.edu, review@msen.com, com-priv@uu.psi.com, gopher-news\n" +
			"Subject: University of Minnesota Gopher software licensing policy.\n" +
			"\n" +
			"In the best of USENET tradition there has been a lot of hysteria,\n" +
			"misinformation, and rumor floating around that this note is being\n" +
			"written to address.  Please treat this as an \"official\" University\n" +
			"of Minnesota Gopher Team position.  We'll put this document up so\n" +
			"that anyone can get a copy before they howl.\n" +
			"\n" +
			"In a time where we are having budgets slashed, it is impossible\n" +
			"to justify continued (increasing) resources being allocated to\n" +
			"Gopher development unless some good things result for\n" +
			"the University of Minnesota.  This is a fact of life.\n" +
			"\n" +
			"We can make a case that if you put up a gopher server that\n" +
			"makes useful information available to the Internet, then there\n" +
			"is more useful information available to the University of\n" +
			"Minnesota academic community also.  Hence this is a Good Thing.\n" +
			"\n" +
			"If on the other hand you put up a gopher server that is\n" +
			"commercial in nature and either inaccessible to the world or\n" +
			"containing information whose primary purpose is to MAKE YOU\n" +
			"MONEY, then we have a hard time making a case for our\n" +
			"admistrators supporting this.  Indeed if you look at this\n" +
			"honestly, a licence fee is the right and proper thing to do.\n" +
			"\n" +
			"Remember when UNIX was given away free?\n" +
			"How many of you are using UNIX now?  It is licensed.\n" +
			"\n" +
			"First, in the case of gopher servers run by higher education\n" +
			"or non-profit organizations offering information freely\n" +
			"accessible to the Internet, there is no change.  No fees.\n" +
			"They just continue to use Gopher like they have always\n" +
			"done.  If you fall under this category, please stop\n" +
			"and think about it.  Nothing's changed.\n" +
			"\n" +
			"In the case where gopher servers are being used internally\n" +
			"by commercial entities we think a license fee is right.\n" +
			"We don't know what amount of a fee is reasonable: so YOU\n" +
			"have to tell us and we need to negotiate on a case by case\n" +
			"basis.  What is loose change for a large corporation may\n" +
			"be prohibitive for a small business.  We'd like some\n" +
			"kind of sliding scale.\n" +
			"\n" +
			"In the case of gopher servers offering information that is\n" +
			"sold, again we think a fee is reasonable and further that\n" +
			"it be some small fraction of your sales.  Once more, we\n" +
			"need to negotiate a reasonable fraction on a case by case\n" +
			"basis.  So comparing YOUR agreement with the one we make\n" +
			"with the guy next door might not be a fruitful thing to do.\n" +
			"\n" +
			"Finally, there is the grey area where information on a\n" +
			"server run by a commercial entity is accessible to all.\n" +
			"Now having price-lists of your products (for example)\n" +
			"available, really is a direct benefit to you.  On the\n" +
			"other hand, while having usefully compiled lists or indexed\n" +
			"journals may well be an indirect benefit to you (folks\n" +
			"will think well of your company and services) they have\n" +
			"a direct benefit to everyone.  In these cases, we'd like\n" +
			"YOU to make a case arguing that the material on your\n" +
			"server falls into the second category, enabling us to\n" +
			"give you a license without a fee.\n" +
			"\n" +
			"Yes, it may seem unfair that we get to decide whether\n" +
			"your commercial server should be given a license to use\n" +
			"our software without a charge.... but there it is.\n" +
			"\n" +
			"We are not out to make big money here.  We are simply\n" +
			"facing the realities of our environment and having to\n" +
			"justify how we spend OUR resources also.\n" +
			"\n" +
			"The Internet Gopher protocol is documented and we're\n" +
			"also just about done with an informational RFC.\n" +
			"Folks can and have written clients and servers for\n" +
			"Gopher.  You can also do it.\n" +
			"\n" +
			"Before you go off and flame once more, ask yourself if\n" +
			"you want to get YOUR particular server going with as\n" +
			"little fuss and expense as possible... or if you just\n" +
			"want to stir up the soup.  Then do what you wish.\n" +
			"We want to keep things working for all of you, and get\n" +
			"ourselves the okays from above to keep doing that.\n" +
			"\n" +
			"If you want to be productive, talk to Shih Pau Yen;\n" +
			"he's the one with the power to do the deals.  He\n" +
			"can be reached at\n" +
			"     yen@boombox.micro.umn.edu\n" +
			"or   (612) 624-8865\n" +
			"\n" +
			"Please don't abuse the Gopher Development team :-)\n" +
			"\n" +
			"  - Yen and the Minnesota Gopher Team\n";

	private static final URL url;

	static{
		try{
			ProtoPack.install();
			url = new URL("gopher://gopher.meulie.net/M/gopher/gopher-software-licensing-policy.ancient");
		}catch(MalformedURLException e){
			throw new IllegalStateException(e);
		}
	}

	public static void main(String[] args){
		new RFC822Parser()
				.parse(url, message, ParserException::printStackTrace)
				.stream()
				.filter(Objects::nonNull)
				.forEach(System.out::println);
	}
}
