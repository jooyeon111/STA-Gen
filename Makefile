all: rtl test

rtl:
	sbt "runMain STA_Gen.SystolicTensorArrayRtlMain"

test:
	sbt "test"

clean:
	#rm *.json *.v
	if [ -d $(CURDIR)/*.json ]; then \
		rm -r *.json; \
	else \
	  echo "There is no json file";\
 	 fi

	if [ -d $(CURDIR)/*.v ]; then \
		rm -r *.v; \
	else \
	  echo "There is no verilog file";\
 	fi


	if [ -d $(CURDIR)/test_run_dir/ ]; then \
		rm -rf $(CURDIR)/test_run_dir/*; \
	else \
	  echo " There is no test_run_dir directory"; \
  	fi
