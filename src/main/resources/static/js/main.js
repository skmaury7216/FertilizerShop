document.addEventListener('DOMContentLoaded', () => {
 const d = document.querySelector('#today');
 if(d) d.textContent = new Date().toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'});
 document.querySelectorAll('[data-filter]').forEach(input => {
  input.addEventListener('input', () => {
   const q = input.value.trim().toLowerCase();
   document.querySelectorAll('#'+input.dataset.filter+' tbody tr').forEach(row => {
    row.style.display=row.textContent.toLowerCase().includes(q)?'':'none';
   });
  });
 });
 const lines=document.getElementById('invoice-lines');
 if(lines){
  const add=document.getElementById('add-invoice-product');
  const total=document.getElementById('sale-total');
  const due=document.getElementById('invoice-due');
  const paid=document.getElementById('invoice-paid');
  const scan=document.getElementById('sale-barcode-scan');
  const status=document.getElementById('sale-barcode-status');
  const form=document.getElementById('multi-sale-form');
  function recalc(){
   let sum=0;
   lines.querySelectorAll('.invoice-line').forEach(row=>{
    const q=Number(row.querySelector('.invoice-quantity').value)||0;
    const p=Number(row.querySelector('.invoice-price').value)||0;
    const value=Math.round(q*p*100)/100;
    row.querySelector('.invoice-line-total').textContent='₹'+value.toFixed(2);
    sum+=value;
   });
   total.textContent='₹'+sum.toFixed(2);
   due.textContent='₹'+(sum-(Number(paid.value)||0)).toFixed(2);
  }
  function selectProduct(row, option){
   const prod=row.querySelector('.invoice-product');
   prod.value=option.value;
   row.querySelector('.invoice-price').value=option.dataset.price||'';
   recalc();
  }
  add.addEventListener('click',()=>{
   if(lines.children.length>=50){ alert('Maximum 50 product rows per invoice'); return; }
   const first=lines.querySelector('.invoice-line');
   const row=first.cloneNode(true);
   row.querySelector('.invoice-product').value='';
   row.querySelector('.invoice-price').value='';
   row.querySelector('.invoice-quantity').value=1;
   row.querySelector('.invoice-line-total').textContent='₹0.00';
   lines.appendChild(row);recalc();
  });
  lines.addEventListener('click',e=>{
   if(!e.target.closest('.invoice-remove'))return;
   if(lines.children.length===1){alert('At least one product is required');return;}
   e.target.closest('.invoice-line').remove();recalc();
  });
  lines.addEventListener('change',e=>{
   if(e.target.matches('.invoice-product')){
    const option=e.target.selectedOptions[0];
    if(option && option.dataset.price)e.target.closest('.invoice-line').querySelector('.invoice-price').value=option.dataset.price;
   }
   recalc();
  });
  lines.addEventListener('input',recalc);paid.addEventListener('input',recalc);
  function scanBarcode(){
   const barcode=scan.value.trim();if(!barcode)return;
   const option=Array.from(lines.querySelector('.invoice-product').options)
    .find(x=>x.dataset.barcode===barcode);
   if(!option){status.textContent='Barcode not found. Save it in Products first.';status.style.color='#b42318';return;}
   const already=Array.from(lines.querySelectorAll('.invoice-line'))
    .find(row=>row.querySelector('.invoice-product').value===option.value);
   if(already){already.querySelector('.invoice-quantity').value=Number(already.querySelector('.invoice-quantity').value||0)+1;}
   else{
    let row=Array.from(lines.querySelectorAll('.invoice-line')).find(row=>!row.querySelector('.invoice-product').value);
    if(!row){if(lines.children.length>=50){alert('Maximum 50 product rows');return;}add.click();row=lines.lastElementChild;}
    selectProduct(row,option);
   }
   status.textContent='Added: '+option.textContent.trim();status.style.color='#16856c';
   scan.value='';recalc();
  }
  if(scan) {
   scan.addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();scanBarcode();}});
   scan.addEventListener('change',scanBarcode);
  }
  form.addEventListener('submit',e=>{
   const selections=Array.from(lines.querySelectorAll('.invoice-product')).map(x=>x.value);
   if(selections.some(x=>!x)||new Set(selections).size!==selections.length){
    e.preventDefault();alert('Select each product only once. Increase quantity for additional units.');return;
   }
   if(Number(paid.value)>Number(total.textContent.replace(/[₹,]/g,''))){
    e.preventDefault();alert('Paid amount exceeds invoice total');
   }
  });
  recalc();
 }
 // Stock Purchase single-product barcode support
 const scan=document.getElementById('purchase-barcode-scan');
 const select=document.getElementById('purchase-product');
 const status=document.getElementById('purchase-barcode-status');
 if(scan && select && status){
  const run=()=>{
   const code=scan.value.trim();if(!code)return;
   const option=Array.from(select.options).find(x=>x.dataset.barcode===code);
   if(!option){status.textContent='Barcode not found';status.style.color='#b42318';return;}
   select.value=option.value;select.dispatchEvent(new Event('change',{bubbles:true}));
   status.textContent='Selected: '+option.textContent.trim();status.style.color='#16856c';scan.value='';
  };
  scan.addEventListener('keydown',e=>{if(e.key==='Enter'){e.preventDefault();run();}});
  scan.addEventListener('change',run);
 }
});
