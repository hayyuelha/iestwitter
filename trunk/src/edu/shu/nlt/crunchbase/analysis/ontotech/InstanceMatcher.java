package edu.shu.nlt.crunchbase.analysis.ontotech;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.nlt.shallow.data.tags.Word;
import edu.nlt.shallow.parser.WordTokenizer;
import edu.shu.nlt.crunchbase.data.base.Company;
import edu.shu.nlt.crunchbase.data.base.Employee;
import edu.shu.nlt.crunchbase.data.base.Person;
import edu.shu.nlt.crunchbase.data.base.Product;
import edu.shu.nlt.crunchbase.data.expanded.CompanyInfo;
import edu.shu.nlt.crunchbase.data.lists.CompanyList;
import edu.shu.nlt.crunchbase.data.lists.PersonList;
import edu.shu.nlt.crunchbase.data.lists.ProductList;

/**
 * Matches string with company/product/person instance
 * 
 * Not thread safe
 * 
 * @author shu
 * 
 */
public class InstanceMatcher {
	private CompanyList companyList;

	private PersonList personList;
	private ProductList productList;

	private StopWords stopWords = new StopWords();
	private WordTokenizer tokenizer = new WordTokenizer(false);

	private void matchCompanyProduct(String phrase, LinkedList<Person> personMatches,
			LinkedList<Company> companyMatches, LinkedList<Product> productMatches) {

		Company company = companyList.getCompany(phrase);
		Product product = productList.getProduct(phrase);
		Person person = null;

		// split firstname + lastName
		String[] words = phrase.split(" ");
		if (words.length >= 2)
			person = personList.getPerson(words[words.length - 2], words[words.length - 1]);

		if (person != null)
			personMatches.add(person);

		if (company != null)
			companyMatches.add(company);

		if (product != null)
			productMatches.add(product);

	}

	public InstanceMatcher() {
		initialize();
	}

	private final int c_minCompanyEmployees = 10;

	/**
	 * Constructs lists of Companies with > X employees, their products
	 */
	private void initialize() {
		CompanyList allCcompanyList = CompanyList.getInstance(new File("data/crunchbase/companies.js"));

		int totalCompaniesProcessed = 0;

		companyList = new CompanyList();
		personList = new PersonList();
		productList = new ProductList();

		for (Company company : allCcompanyList.getCompanies()) {
			totalCompaniesProcessed++;
			CompanyInfo companyInfo = company.getCompanyInfo();

			if (companyInfo != null && companyInfo.getEmployees().size() >= c_minCompanyEmployees) {

				System.out.println("Processing company: " + company.getName() + "  total processed: "
						+ totalCompaniesProcessed);

				companyList.addCompany(company);

				for (Employee employee : companyInfo.getEmployees()) {
					personList.addPerson(employee.getPerson());
				}

				for (Product product : companyInfo.getProducts()) {
					productList.addProduct(product);
				}
			}
		}
	}

	public static void main(String[] args) {

		InstanceMatcher matcher = new InstanceMatcher();

		MatchResult results = matcher.match("Mark  is the founder of Facebook, which makes the Facebook Platform");

		for (Person person : results.getPersonMatches())
			person.printDetails(System.out);

		for (Company company : results.getCompanyMatches())
			company.printDetails(System.out);

		for (Product product : results.getProductMatches())
			product.printDetails(System.out);

	}

	public MatchResult match(String sentence) {
		List<Word> words = new ArrayList<Word>(tokenizer.getWords(sentence));

		LinkedList<Company> companyMatches = new LinkedList<Company>();
		LinkedList<Product> productMatches = new LinkedList<Product>();
		LinkedList<Person> personMatches = new LinkedList<Person>();

		for (int i = 0; i < words.size(); i++) {

			Word currentWord = words.get(i);

			String matchWith = currentWord.toString();

			// match unigram & ignore stop words
			if (!stopWords.isStopWord(matchWith)) {
				matchCompanyProduct(matchWith, personMatches, companyMatches, productMatches);
			}

			// match bigram
			if (i > 0) {
				matchWith = words.get(i - 1) + " " + matchWith;
				matchCompanyProduct(matchWith, personMatches, companyMatches, productMatches);

				// match tri-gram
				if (i > 1) {
					matchWith = words.get(i - 2) + " " + matchWith;
					matchCompanyProduct(matchWith, personMatches, companyMatches, productMatches);
				}
			}
		}

		return new MatchResult(companyMatches, personMatches, productMatches);
	}

	public static class MatchResult {

		public List<Company> getCompanyMatches() {
			return companyMatches;
		}

		public List<Person> getPersonMatches() {
			return personMatches;
		}

		public List<Product> getProductMatches() {
			return productMatches;
		}

		public int getTotalMatches() {
			return companyMatches.size() + personMatches.size() + productMatches.size();
		}

		public MatchResult(List<Company> companyMatches, List<Person> personMatches, List<Product> productMatches) {
			super();
			this.companyMatches = companyMatches;
			this.personMatches = personMatches;
			this.productMatches = productMatches;
		}

		private List<Company> companyMatches;
		private List<Person> personMatches;
		private List<Product> productMatches;
	}

}
