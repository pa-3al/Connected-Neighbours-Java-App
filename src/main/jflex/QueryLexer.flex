package com.app.query;
import java_cup.runtime.*;
import com.app.query.sym;
%%
%class QueryLexer
%public
%unicode
%cup
%line
%column
%{
  private Symbol symbol(int type) {
    return new Symbol(type, yyline, yycolumn);
  }
  private Symbol symbol(int type, Object value) {
    return new Symbol(type, yyline, yycolumn, value);
  }
%}
WHITESPACE = [ \t\r\n]+
IDENTIFIER = [a-zA-Z_][a-zA-Z0-9_]*
STRING_LITERAL = \"[^\"]*\"
%%
<YYINITIAL> {
  "SELECT"       { return symbol(sym.SELECT); }
  "FROM"         { return symbol(sym.FROM); }
  "WHERE"        { return symbol(sym.WHERE); }
  "*"            { return symbol(sym.STAR); }
  ","            { return symbol(sym.COMMA); }
  ";"            { return symbol(sym.SEMI); }
  "="            { return symbol(sym.EQ); }
  {IDENTIFIER}   { return symbol(sym.ID, yytext()); }
  {STRING_LITERAL} { return symbol(sym.STRING, yytext().substring(1, yytext().length()-1)); }
  {WHITESPACE}   {  }
}
[^] { throw new Error("Illegal character <"+yytext()+">"); }
