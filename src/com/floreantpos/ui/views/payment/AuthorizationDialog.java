package com.floreantpos.ui.views.payment;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.floreantpos.Messages;
import com.floreantpos.config.CardConfig;
import com.floreantpos.main.Application;
import com.floreantpos.model.CardReader;
import com.floreantpos.model.PosTransaction;
import com.floreantpos.model.dao.PosTransactionDAO;
import com.floreantpos.swing.PosButton;
import com.floreantpos.swing.TransparentPanel;
import com.floreantpos.ui.dialog.POSDialog;

class AuthorizationDialog extends POSDialog implements Runnable {
	private JLabel label;
	private JTextArea txtStatus;
	private PosButton btnFinish;

	private List<PosTransaction> transactions;

	public AuthorizationDialog(POSDialog parent, List<PosTransaction> transactions) {
		this.transactions = transactions;

		initComponents();
		setTitle(Messages.getString("PaymentProcessWaitDialog.0")); //$NON-NLS-1$
		setIconImage(Application.getPosWindow().getIconImage());
		setLocationRelativeTo(parent);
	}

	@Override
	public void setVisible(boolean b) {
		if(b) {
			Thread authorizationThread = new Thread(this);
			authorizationThread.start();
		}

		super.setVisible(b);
	}

	@Override
	public void run() {
		for (Iterator iterator = transactions.iterator(); iterator.hasNext();) {
			PosTransaction transaction = (PosTransaction) iterator.next();
			try {
				String cardEntryType = transaction.getCardReader();
				CardReader cardReader = CardReader.fromString(cardEntryType);

				if(cardReader == CardReader.EXTERNAL_TERMINAL) {
					transaction.setCaptured(true);
					PosTransactionDAO.getInstance().saveOrUpdate(transaction);
					txtStatus.append(Messages.getString("AuthorizationDialog.1") + transaction.getId() + Messages.getString("AuthorizationDialog.2"));  //$NON-NLS-1$ //$NON-NLS-2$
					continue;
				}

				CardProcessor cardProcessor = CardConfig.getPaymentGateway().getProcessor();
				cardProcessor.captureAuthorizedAmount(transaction);
				transaction.setCaptured(true);

				PosTransactionDAO.getInstance().saveOrUpdate(transaction);
				txtStatus.append(Messages.getString("AuthorizationDialog.1") + transaction.getId() + Messages.getString("AuthorizationDialog.2"));  //$NON-NLS-1$ //$NON-NLS-2$

				if(iterator.hasNext()) {
					Thread.sleep(6000);
				}

			} catch (InterruptedException x) {

			} catch (Exception e) {
				System.out.println(CardConfig.getPaymentGateway().getName());
				txtStatus.append(Messages.getString("AuthorizationDialog.1") + transaction.getId() + Messages.getString("AuthorizationDialog.4") + e.getMessage() + "\n");  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}

		label.setText(Messages.getString("AuthorizationDialog.0")); //$NON-NLS-1$
		btnFinish.setVisible(true);
	}

	private void initComponents() {
		TransparentPanel transparentPanel1 = new TransparentPanel();
		transparentPanel1.setLayout(new BorderLayout());
		transparentPanel1.setOpaque(true);

		label = new JLabel(Messages.getString("PaymentProcessWaitDialog.1") + CardConfig.getPaymentGateway().getName() + Messages.getString("PaymentProcessWaitDialog.8"));  //$NON-NLS-1$ //$NON-NLS-2$
		label.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(label.getFont().deriveFont(24).deriveFont(Font.BOLD));

		txtStatus = new JTextArea();
		txtStatus.setEditable(false);
		txtStatus.setLineWrap(true);

		transparentPanel1.add(label, BorderLayout.NORTH);
		JScrollPane scrollPane = new JScrollPane(txtStatus);
		scrollPane.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(20, 30, 20, 30), scrollPane.getBorder()));
		transparentPanel1.add(scrollPane, BorderLayout.CENTER);

		TransparentPanel transparentPanel2 = new TransparentPanel();
		transparentPanel2.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

		btnFinish = new PosButton();
		btnFinish.setText(com.floreantpos.POSConstants.FINISH);
		btnFinish.setPreferredSize(new Dimension(140, 50));
		btnFinish.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				doFinish(evt);
			}
		});
		btnFinish.setVisible(false);

		transparentPanel2.add(btnFinish);

		transparentPanel1.add(transparentPanel2, BorderLayout.SOUTH);

		getContentPane().add(transparentPanel1, BorderLayout.CENTER);

		setSize(500, 400);
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		setResizable(false);
	}

	private void doFinish(ActionEvent evt) {
		dispose();
	}

}