package com.dianping.cat.consumer.cross;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.unidal.helper.Files;
import org.unidal.lookup.ComponentTestCase;

import com.dianping.cat.Constants;
import com.dianping.cat.analysis.MessageAnalyzer;
import com.dianping.cat.consumer.cross.model.entity.CrossReport;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.internal.DefaultEvent;
import com.dianping.cat.message.internal.DefaultTransaction;
import com.dianping.cat.message.spi.MessageTree;
import com.dianping.cat.message.spi.internal.DefaultMessageTree;

public class CrossAnalyzerTest extends ComponentTestCase {

	private long m_timestamp;

	private CrossAnalyzer m_analyzer;

	private String m_domain = "group";

	@Before
	public void setUp() throws Exception {
		super.setUp();
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
		long currentTimeMillis = System.currentTimeMillis();

		m_timestamp = currentTimeMillis - currentTimeMillis % (3600 * 1000);

		m_analyzer = (CrossAnalyzer) lookup(MessageAnalyzer.class, CrossAnalyzer.ID);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");
		Date date = sdf.parse("20120101 00:00");

		m_analyzer.initialize(date.getTime(), Constants.HOUR, Constants.MINUTE * 5);
	}

	@Test
	public void testProcess() throws Exception {
		for (int i = 1; i <= 100; i++) {
			MessageTree tree = generateMessageTree(i);

			m_analyzer.process(tree);
		}

		CrossReport report = m_analyzer.getReport(m_domain);

		String expected = Files.forIO().readFrom(getClass().getResourceAsStream("cross_analyzer.xml"), "utf-8");
		Assert.assertEquals(expected.replaceAll("\r", ""), report.toString().replaceAll("\r", ""));
	}

	protected MessageTree generateMessageTree(int i) {
		MessageTree tree = new DefaultMessageTree();

		tree.setMessageId("" + i);
		tree.setDomain(m_domain);
		tree.setHostName("group001");
		tree.setIpAddress("192.168.1.1");

		DefaultTransaction t;

		if (i % 2 == 0) {
			t = new DefaultTransaction("PigeonCall", "Cat-Test-Call", null);
			DefaultEvent event = new DefaultEvent("PigeonCall.server", "192.168.1.0:3000:class:method1");

			event.setTimestamp(m_timestamp + 5 * 60 * 1000);
			event.setStatus(Message.SUCCESS);
			t.addChild(event);
		} else {
			t = new DefaultTransaction("PigeonService", "Cat-Test-Service", null);
			DefaultEvent event = new DefaultEvent("PigeonService.client", "192.168.1.2:3000:class:method2");

			event.setTimestamp(m_timestamp + 5 * 60 * 1000);
			event.setStatus(Message.SUCCESS);
			t.addChild(event);
		}

		t.complete();
		t.setDurationInMillis(i * 2);
		t.setTimestamp(m_timestamp + 1000);
		tree.setMessage(t);

		return tree;
	}

	@Test
	public void testFormatIp() {
		IpConvertManager analyzer = new IpConvertManager();

		Assert.assertEquals(true, analyzer.isIPAddress("10.1.6.128"));
		Assert.assertEquals(false, analyzer.isIPAddress("10.1.6.328"));
		Assert.assertEquals(false, analyzer.isIPAddress("2886.1.6.128"));
		Assert.assertEquals(false, analyzer.isIPAddress("2886.1.6.1228"));

		Assert.assertEquals("10.1.6.128", analyzer.convertHostNameToIP("10.1.6.128"));
	}

}
