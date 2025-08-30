const fs = require('fs');
const path = require('path');

// Import md2pdf - this will be available after npm install
let md2pdf;
try {
    md2pdf = require('md2pdf');
} catch (e) {
    console.error('md2pdf module not found. Run: npm install md2pdf');
    process.exit(1);
}

async function convert() {
    try {
        // Read arguments from command line
        const args = process.argv.slice(2);
        if (args.length < 2) {
            console.error('Usage: node bridge.js <input-file> <options-json>');
            process.exit(1);
        }
        
        const inputFile = args[0];
        const optionsJson = args[1];
        
        // Parse options
        const options = JSON.parse(optionsJson);
        
        // Read markdown content
        const markdown = fs.readFileSync(inputFile, 'utf8');
        
        // Configure conversion options
        const md2pdfOptions = {
            pdf_options: {
                format: options.pageSize || 'A4',
                margin: parseMargin(options.margin),
                printBackground: true,
                timeout: (options.timeout || 60) * 1000
            },
            css_style: buildCssStyle(options),
            highlight_style: options.theme || 'github'
        };
        
        // Perform conversion
        const pdfBuffer = await md2pdf.convert(markdown, md2pdfOptions);
        
        // Write output
        const outputPath = options.output;
        if (outputPath === '-') {
            process.stdout.write(pdfBuffer);
        } else {
            fs.writeFileSync(outputPath, pdfBuffer);
        }
        
        console.log('Conversion successful');
        process.exit(0);
    } catch (error) {
        console.error('Conversion failed:', error.message);
        process.exit(1);
    }
}

function parseMargin(marginStr) {
    if (!marginStr) return undefined;
    
    const parts = marginStr.split(',').map(s => s.trim());
    if (parts.length === 1) {
        return { top: parts[0], right: parts[0], bottom: parts[0], left: parts[0] };
    } else if (parts.length === 4) {
        return { top: parts[0], right: parts[1], bottom: parts[2], left: parts[3] };
    }
    return undefined;
}

function buildCssStyle(options) {
    let css = '';
    
    // Add custom CSS file if provided
    if (options.css) {
        try {
            css += fs.readFileSync(options.css, 'utf8') + '\n';
        } catch (e) {
            console.warn('Warning: Could not read CSS file:', options.css);
        }
    }
    
    // Add theme-specific CSS
    if (options.theme && options.theme !== 'default') {
        css += getThemeCss(options.theme);
    }
    
    return css || undefined;
}

function getThemeCss(theme) {
    // Basic built-in themes
    switch (theme) {
        case 'github':
            return `
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; }
                h1, h2, h3, h4, h5, h6 { color: #24292e; border-bottom: 1px solid #eaecef; }
                code { background-color: rgba(27,31,35,0.05); padding: 0.2em 0.4em; }
                pre { background-color: #f6f8fa; padding: 16px; border-radius: 6px; }
            `;
        case 'classic':
            return `
                body { font-family: 'Times New Roman', serif; max-width: 800px; margin: 0 auto; }
                h1, h2, h3, h4, h5, h6 { color: #333; }
                code { background-color: #f4f4f4; padding: 2px 4px; }
                pre { background-color: #f8f8f8; padding: 12px; border: 1px solid #ddd; }
            `;
        default:
            return '';
    }
}

// Execute conversion
convert();


