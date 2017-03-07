package org.inacio;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.inacio.Individual;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

public class IndividualIO {
	final static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	final static Logger LOG = Logger.getLogger(IndividualIO.class.getName());

	public static Individual get(String p) {
		Individual individual = new Individual();
		individual.setId(KeyFactory.stringToKey(p));
		get(individual);
		return individual;
	}
	
	public static void get(Individual m) {
		Entity e;
		if(m.getId() == null) {
			return;
		}
		Key k = m.getId();
		try {
			e = datastore.get(k);
			m = setIndividual(e);
		} catch (EntityNotFoundException e1) {
			LOG.log(Level.SEVERE,e1.getMessage());
		}
	}
	
	public static ArrayList<Individual> getIndividualsOfMember(String s)
	{
		return getIndividualsOfMember(KeyFactory.stringToKey(s));
	}
	public static ArrayList<Individual> getIndividualsOfMember(Key k)
	{
		ArrayList<Individual> i = new ArrayList<Individual>();		
		Filter filter = new FilterPredicate("memberid",FilterOperator.EQUAL,k);
		Query q = new Query(Individual.class.getName()).setAncestor(k).setFilter(filter);
		PreparedQuery pq = datastore.prepare(q);
		List<Entity> le = pq.asList(FetchOptions.Builder.withDefaults());
		for(Entity e: le)
		{
			i.add(setIndividual(e));
		}
		return i;
	}
	public static Individual setIndividual(Entity e)
	{
		Individual i = null;
		if(e != null) {
			i = new Individual();
			i.setMemberId((Key) e.getProperty("memberid"));
			i.setId(e.getKey());
			i.setName((String) e.getProperty("name"));
			i.setDob((Long) e.getProperty("dob"));
			i.setAdult((boolean) (Common.getAge(i.getDob()) > 17));
			i.setGender((String) e.getProperty("gender"));			
		}
		return i;		
	}
    public static Key add(Individual i) {
		Entity e = null;
		if(i.getId() != null) {
			try {
				e = datastore.get(i.getId());
			} catch (EntityNotFoundException e1) {
				LOG.log(Level.SEVERE, e1.getMessage());
				e = null;
			}
		}
		if( e == null) { // Handle as new add
			e = new Entity(Individual.class.getName(), i.getMemberId());
		}
    	e.setProperty("dob", i.getDob());
    	e.setProperty("gender", i.getGender());
    	e.setProperty("memberid", i.getMemberId());
    	e.setProperty("name", i.getName());
    	Key k = datastore.put(e);
    	i.setId(k);
    	return k;
    }
}
