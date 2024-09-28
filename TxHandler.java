import java.util.*; // For HashSet data structure
public class TxHandler {


	private UTXOPool utxoPool;
	/* Creates a public ledger whose current UTXOPool (collection of unspent
	 * transaction outputs) is utxoPool. This should make a defensive copy of
	 * utxoPool by using the UTXOPool(UTXOPool uPool) constructor.
	 */
	public TxHandler(UTXOPool utxoPool) {
		this.utxoPool = new UTXOPool(utxoPool);
	}


	/* Returns true if
	 * (1) all outputs claimed by tx are in the current UTXO pool,
	 * (2) the signatures on each input of tx are valid,
	 * (3) no UTXO is claimed multiple times by tx,
	 * (4) all of tx’s output values are non-negative, and
	 * (5) the sum of tx’s input values is greater than or equal to the sum of
	 its output values;
	 and false otherwise.
	 */

	public boolean isValidTx(Transaction tx) {
		HashSet<UTXO> seenUTXO = new HashSet<UTXO>();
		double inputVal = 0.0;
		double outputVal = 0.0;
		int index = 0;

		for(Transaction.Input in : tx.getInputs()){
			UTXO ut = new UTXO(in.prevTxHash, in.outputIndex);
			// Step 1
			if (!this.utxoPool.contains(ut)){
				return false;
			}

			double prevOutVal = utxoPool.getTxOutput(ut).value;
			inputVal += prevOutVal;

			// Step 3
			if (seenUTXO.contains(ut)){
				return false;
			}
			seenUTXO.add(ut);

			// Step 2
			if(!utxoPool.getTxOutput(ut).address.verifySignature(tx.getRawDataToSign(index), in.signature)){
				return false;
			}
			index++;
		}

		// Step 4
		for (Transaction.Output out : tx.getOutputs()){
			if (out.value < 0.0) {
				return false;
			}
			outputVal += out.value;
		}

		// Step 5
		if (outputVal > inputVal){
			return false;
		}

		return true;
	}

	/* Handles each epoch by receiving an unordered array of proposed
	 * transactions, checking each transaction for correctness,
	 * returning a mutually valid array of accepted transactions,
	 * and updating the current UTXO pool as appropriate.
	 */
	public Transaction[] handleTxs(Transaction[] possibleTxs) {
		// (1) Return only valid transactions
		// (2) One transaction's inputs may depend on the output of another
		// transaction in the same epoch
		// (3) Update uxtoPool
		// (4) Return mutally valid transaction set of maximal size

		HashSet<Transaction> trans = new HashSet<Transaction>(Arrays.asList(possibleTxs));
		int transCount = 0;
		ArrayList<Transaction> valid = new ArrayList<Transaction>();

		do {
			transCount = trans.size();
			HashSet<Transaction> toRemove = new HashSet<Transaction>();
			for (Transaction tx : trans) {

				// Step 1
				if(!isValidTx(tx)) {
					continue;
				}

				// Step 3
				valid.add(tx);
				updatePool(tx);
				toRemove.add(tx);

			}
			
			for (Transaction tx : toRemove){
				trans.remove(tx);
			}


		} while (transCount != trans.size()  && transCount != 0);

		// Step 4
		return valid.toArray(new Transaction[valid.size()]);
	}

	private void updatePool(Transaction tx){

		for(Transaction.Input input : tx.getInputs()) {
			UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
			this.utxoPool.removeUTXO(utxo);
		}

		byte[] txHash = tx.getHash();
		int index = 0;
		for (Transaction.Output output : tx.getOutputs()) {
			UTXO utxo = new UTXO(txHash, index);
			index++;
			this.utxoPool.addUTXO(utxo,output);
		}
	}
}
