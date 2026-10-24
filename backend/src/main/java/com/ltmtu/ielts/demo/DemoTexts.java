package com.ltmtu.ielts.demo;

import java.util.List;

/** Đề bài và bài làm mẫu cho dữ liệu demo — tự soạn, không lấy từ đề thi thật. */
final class DemoTexts {

	private DemoTexts() {
	}

	static final String TASK1_PROMPT = """
			<p>The line graph shows the number of visitors to three museums in a city (the Science Museum, \
			the History Museum and the Art Gallery) between 2010 and 2020.</p>
			<p>Summarise the information by selecting and reporting the main features, and make comparisons \
			where relevant.</p>""";

	static final String TASK2_PROMPT = """
			<p>Some people think that students should study only the subjects they are interested in, \
			while others believe they should study a wide range of subjects.</p>
			<p>Discuss both views and give your own opinion.</p>""";

	static final String TASK2_PROMPT_WEEK2 = """
			<p>In many cities, private cars are being banned from the city centre.</p>
			<p>Do the advantages of this policy outweigh the disadvantages?</p>""";

	/** Bài Task 1 đã nộp, dùng xoay vòng cho các sinh viên mẫu. */
	static final List<String> TASK1_ANSWERS = List.of("""
			The line graph compares how many people visited three museums in a city over a ten-year period \
			starting in 2010.

			Overall, the Science Museum was the most popular attraction for most of the period, while the Art \
			Gallery attracted the fewest visitors. The History Museum was the only place where numbers fell.

			In 2010, the Science Museum welcomed around 400,000 visitors, roughly twice the figure for the Art \
			Gallery. Its numbers rose steadily and peaked at about 650,000 in 2018, before dipping slightly in \
			the final two years.

			The History Museum started at a similar level to the Science Museum, at approximately 380,000. \
			However, it saw a gradual decline throughout the decade and ended the period with just under \
			250,000 visitors.

			The Art Gallery remained the least visited museum until 2015, with figures fluctuating around \
			200,000. After that, it grew rapidly and overtook the History Museum in 2017, finishing at nearly \
			350,000 visitors in 2020.""", """
			The graph show the number of visitor to three museum in a city from 2010 to 2020.

			In general the science museum have the most visitor and the art gallery have the least visitor at \
			the beginning. The history museum go down.

			In 2010 the science museum had 400000 visitors and it increase to 650000 in 2018. Then it go down a \
			little bit to 600000 in 2020. The history museum had 380000 in 2010 but it decrease every year and \
			in 2020 it only had 250000 visitors.

			The art gallery had 200000 visitors in 2010 and it was stable until 2015. After 2015 the number \
			increase very fast and it became more than history museum in 2017. In 2020 it had 350000 visitors. \
			In conclusion, science museum was most popular and history museum was less popular in the end of \
			the period.""", """
			This line chart illustrates changes in visitor numbers at the Science Museum, the History Museum \
			and the Art Gallery from 2010 to 2020.

			It is clear that the Science Museum remained the most visited of the three, whereas the History \
			Museum experienced a downward trend. The Art Gallery, by contrast, showed the most dramatic growth.

			At the start of the period, the Science Museum and the History Museum had similar numbers of \
			visitors, at about 400,000 and 380,000 respectively. Over the following eight years, visits to the \
			Science Museum climbed to a high of 650,000, while those to the History Museum fell continuously, \
			reaching around 250,000 by 2020.

			Meanwhile, the Art Gallery attracted only about 200,000 people a year until 2015. From then on, its \
			popularity increased sharply, so that it had more visitors than the History Museum from 2017 \
			onwards and almost 350,000 in the last year.""");

	/** Bản nháp Task 2 đang viết dở. */
	static final String TASK2_DRAFT = """
			It is often argued that students should focus only on the subjects they enjoy. Others, however, \
			believe that a broad education is more valuable. This essay will discuss both views before \
			explaining why I believe""";

	private static final String[] FAMILY = { "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Đặng", "Bùi", "Đỗ",
			"Ngô" };
	private static final String[] MIDDLE = { "Văn", "Thị", "Minh", "Thu", "Quốc", "Ngọc", "Đức", "Thanh", "Hải",
			"Gia" };
	private static final String[] GIVEN = { "An", "Bình", "Chi", "Dũng", "Giang", "Hà", "Khánh", "Linh", "Nam",
			"Phương", "Quân", "Trang", "Tuấn", "Vy" };

	/** Họ tên tiếng Việt cố định theo số thứ tự (chạy lại ra cùng tên). */
	static String studentName(int n) {
		return FAMILY[n % FAMILY.length] + " " + MIDDLE[(n / 3) % MIDDLE.length] + " " + GIVEN[n % GIVEN.length];
	}
}
