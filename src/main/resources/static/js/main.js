document.addEventListener('DOMContentLoaded',()=>{
 const d=document.querySelector('#today');if(d)d.textContent=new Date().toLocaleDateString('en-IN',{day:'numeric',month:'short',year:'numeric'});
 document.querySelectorAll('[data-filter]').forEach(input=>{input.addEventListener('input',()=>{const q=input.value.trim().toLowerCase();document.querySelectorAll('#'+input.dataset.filter+' tbody tr').forEach(row=>row.style.display=row.textContent.toLowerCase().includes(q)?'':'none');});});
 const prod=document.querySelector('#sale-product'),qty=document.querySelector('#sale-quantity'),price=document.querySelector('#sale-price'),total=document.querySelector('#sale-total');
 if(prod&&qty&&price&&total){prod.addEventListener('change',()=>{const opt=prod.selectedOptions[0];if(opt&&opt.dataset.price)price.value=opt.dataset.price;calc();});qty.addEventListener('input',calc);price.addEventListener('input',calc);function calc(){total.textContent='₹'+((Number(qty.value)||0)*(Number(price.value)||0)).toFixed(2);}}
});
