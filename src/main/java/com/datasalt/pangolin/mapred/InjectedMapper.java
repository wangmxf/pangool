package com.datasalt.pangolin.mapred;

import java.io.IOException;
import java.util.List;

import org.apache.hadoop.mapreduce.Mapper;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

/**
 * A Mapper ready to be injected by Guice.
 *   
 * @author ivan
 */
public abstract class InjectedMapper<IKey, IValue, OKey, OValue> extends Mapper<IKey, IValue, OKey, OValue> {
	
	public InjectedMapper() {
		super();
		
	}

	/**
	 * The setup class is overriden for injecting. Remember to call super.setup() when
	 * overriding
	 */
	@Override
  protected void setup(Context context) throws IOException,
      InterruptedException {
	  super.setup(context);
		Injector inj = Guice.createInjector(getGuiceModules(context));
		inj.injectMembers(this);
  }
	
	/**
	 * Override to provide a list of modules that will be used to create an injector
	 */
	abstract protected List<? extends Module> getGuiceModules(final Context context);
}