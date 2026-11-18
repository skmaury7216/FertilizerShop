document.addEventListener('DOMContentLoaded',()=>{
 const d=document.querySelector('#today');if(d)d.textContent=new Date().toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'});
 document.querySelectorAll('[data-filter]').forEach(input=>{input.addEventListener('input',()=>{const q=input.value.trim().toLowerCase();document.querySelectorAll('#'+input.dataset.filter+' tbody tr').forEach(row=>row.style.display=row.textContent.toLowerCase().includes(q)?'':'none');});});
 const prod=document.querySelector('#sale-product'),qty=document.querySelector('#sale-quantity'),price=document.querySelector('#sale-price'),total=document.querySelector('#sale-total');
 if(prod&&qty&&price&&total){prod.addEventListener('change',()=>{const opt=prod.selectedOptions[0];if(opt&&opt.dataset.price)price.value=opt.dataset.price;calc();});qty.addEventListener('input',calc);price.addEventListener('input',calc);function calc(){total.textContent='₹'+((Number(qty.value)||0)*(Number(price.value)||0)).toFixed(2);}}
});



// Barcode scanners in keyboard (HID) mode type the code and usually press Enter.
// Scanning selects a pre-registered product; it never posts a bill automatically.
function bindBarcodeScanner(inputId, selectId, statusId) {
  const input = document.getElementById(inputId);
  const select = document.getElementById(selectId);
  const status = document.getElementById(statusId);
  if (!input || !select || !status) return;
  function scan() {
    const code = input.value.trim();
    if (!code) return;
    const option = Array.from(select.options).find(opt => opt.dataset.barcode === code);
    if (!option) {
      status.textContent = 'Barcode not found. Assign this code in Products first.';
      status.style.color = '#b42318';
      return;
    }
    select.value = option.value;
    select.dispatchEvent(new Event('change', {bubbles:true}));
    status.textContent = 'Selected: ' + option.textContent.trim();
    status.style.color = '#16856c';
    input.value = '';
  }
  input.addEventListener('keydown', event => {
    if (event.key === 'Enter') {
      event.preventDefault(); // Never submit the sale or purchase on a barcode scan.
      scan();
    }
  });
  input.addEventListener('change', scan);
}
bindBarcodeScanner('sale-barcode-scan', 'sale-product', 'sale-barcode-status');
bindBarcodeScanner('purchase-barcode-scan', 'purchase-product', 'purchase-barcode-status');
