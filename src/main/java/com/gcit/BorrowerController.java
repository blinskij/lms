package com.gcit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.gcit.library.hbm.entities.TblBookCopies;
import com.gcit.library.hbm.entities.TblBookCopiesId;
import com.gcit.library.hbm.entities.TblBookLoans;
import com.gcit.library.hbm.entities.TblBookLoansId;
import com.gcit.library.hbm.entities.TblBorrower;
import com.gcit.library.hbm.entities.TblLibraryBranch;

/**
 * Handles requests for the application home page.
 */
@RestController
public class BorrowerController {

	private static final Logger logger = LoggerFactory
			.getLogger(BorrowerController.class);

	@PersistenceContext
	private EntityManager em;

	//validate the user
	@RequestMapping(value="/borrowers/{cardNo}", method = { RequestMethod.GET}, produces = "application/json")
	public boolean authentificate(@PathVariable Integer cardNo) {
		try {
			return (em.getReference(TblBorrower.class, cardNo) != null);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	//get the list of branches
	@RequestMapping(value = "/branches", method = { RequestMethod.GET }, produces = "application/json")
	public List<TblLibraryBranch> getAllBranches() {
		try {
			@SuppressWarnings("unchecked")
			List<TblLibraryBranch> list = em.createQuery("from TblLibraryBranch").getResultList(); 
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	//get the list of available books for the branch
	@RequestMapping(value = "/bookCopies/{branchId}", method = { RequestMethod.GET }, produces = "application/json")
	public List<TblBookCopies> getAllBookCopies(@PathVariable Integer branchId) {
		try {
			@SuppressWarnings("unchecked")
			List<TblBookCopies> list = em.createQuery("from TblBookCopies where branchId = :branchId and noOfCopies > 0").setParameter("branchId", branchId).getResultList(); 
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	//get the list of current loans filtered by branch and user
	@RequestMapping(value = "/loans/{userId}/{branchId}", method = { RequestMethod.GET }, produces = "application/json")
	public List<TblBookLoans> getAllLoans(@PathVariable Integer userId, Integer branchId) {
		try {
			@SuppressWarnings("unchecked")
			List<TblBookLoans> list = em.createQuery("from TblBookLoans where dateIn IS NULL and id.cardNo = :cardNo and id.branchId = :branchId").setParameter("cardNo", userId).setParameter("branchId", branchId).getResultList(); 
			return list;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	//return a book
	@Transactional
	@RequestMapping(value = "/loan", method = {RequestMethod.PUT }, consumes = "application/json")
	public String returnBook(@RequestBody Integer bookId, Integer branchId, Integer cardNo, String dateOut) {
		try {
			TblBookLoans loan = em.getReference(TblBookLoans.class, new TblBookLoansId(bookId, branchId, cardNo, dateOut));
			loan.setDateIn(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
			em.merge(loan);
			
			//add book copies
			TblBookCopies copies = em.getReference(TblBookCopies.class, new TblBookCopiesId(loan.getTblBook().getBookId(), loan.getTblLibraryBranch().getBranchId()));
			copies.setNoOfCopies(copies.getNoOfCopies() + 1);
			em.merge(copies);
		} catch (Exception e) {
			e.printStackTrace();
			return "Return Book failed: " + e.getMessage();
		}
		return "Book is returned succesfully!";
	}	
	
	//borrow a book
	@Transactional
	@RequestMapping(value = "/loan", method = {RequestMethod.POST }, consumes = "application/json")
	public String borrowBook(@RequestBody TblBookLoans loan) {
		try {
			DateTimeFormatter f = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
			LocalDateTime dateOut = LocalDateTime.now();
			loan.setId(new TblBookLoansId(loan.getTblBook().getBookId(), loan.getTblLibraryBranch().getBranchId(), loan.getTblBorrower().getCardNo(), dateOut.format(f)));
			loan.setDateOut(dateOut.format(f));
			loan.setDueDate(dateOut.plusWeeks(1).format(f));
			
			em.persist(loan);
			
			//remove book copies
			TblBookCopies copies = em.getReference(TblBookCopies.class, new TblBookCopiesId(loan.getTblBook().getBookId(), loan.getTblLibraryBranch().getBranchId()));
			copies.setNoOfCopies(copies.getNoOfCopies() - 1);
			em.merge(copies);
		} catch (Exception e) {
			e.printStackTrace();
			return "Borrowing Book failed: " + e.getMessage();
		}
		return "Book was borrowed succesfully!";
	}
}
