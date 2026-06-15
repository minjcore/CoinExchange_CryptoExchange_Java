/** md-to-pdf config — md2pdf.sh (npm fallback) */
module.exports = {
  stylesheet: ['md2pdf.css'],
  body_class: 'md2pdf',
  pdf_options: {
    format: 'A4',
    margin: { top: '22mm', right: '20mm', bottom: '22mm', left: '20mm' },
    printBackground: true,
  },
};
